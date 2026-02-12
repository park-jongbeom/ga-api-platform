package com.goalmond.api.service.research

import com.goalmond.api.domain.research.ResearchReport
import com.goalmond.api.domain.research.ResearchRequest
import com.goalmond.api.domain.research.ResearchResult
import com.goalmond.api.domain.research.ResearchStage
import com.goalmond.api.service.ai.GeminiApiException
import com.goalmond.api.service.ai.GeminiClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * 범용 조사 에이전트 서비스.
 * GeminiClient 재사용, 단일/단계별/전체 조사 지원.
 */
@Service
class ResearchAgentService(
    private val geminiClient: GeminiClient,
    private val promptRepository: PromptRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val systemPrompt = """
        당신은 미국 유학 전문 컨설턴트입니다.
        
        역할:
        - 미국 교육 시스템에 대한 정확하고 최신의 정보 제공
        - 유학생 관점에서의 실용적이고 현실적인 조언
        - 비자, 취업, 영주권 관련 정확한 법적 정보 안내
        
        응답 원칙:
        1. 항상 구조화된 형식으로 명확하게 응답
        2. 불확실한 정보는 그렇다고 명시
        3. 가능하면 구체적인 숫자와 출처 제시
        4. 유학생에게 중요한 비자/취업 관련 주의사항 강조
        5. 모든 응답은 한국어로 작성
        
        데이터 기준: 2026년 최신 정보 기준으로 응답해주세요.
    """.trimIndent()

    /**
     * 단일 프롬프트 조사 실행.
     */
    fun researchPrompt(promptId: String, variables: Map<String, Any>): ResearchResult {
        val template = promptRepository.findById(promptId)
            ?: throw IllegalArgumentException("프롬프트를 찾을 수 없습니다: $promptId")

        val renderedPrompt = template.render(variables)
        logger.info("프롬프트 실행: $promptId")

        return try {
            val answer = callLLM(renderedPrompt)
            ResearchResult(
                promptId = promptId,
                stageName = template.stage.displayName,
                question = renderedPrompt.take(300),
                answer = answer
            )
        } catch (e: GeminiApiException) {
            logger.error("프롬프트 $promptId 실행 실패: ${e.message}")
            ResearchResult(
                promptId = promptId,
                stageName = template.stage.displayName,
                question = renderedPrompt.take(300),
                answer = "AI 응답 생성 실패: ${e.message}",
                confidence = 0.0
            )
        }
    }

    /**
     * 특정 단계의 모든 프롬프트 조사.
     */
    fun researchStage(
        category: String,
        stage: ResearchStage,
        variables: Map<String, Any>
    ): List<ResearchResult> {
        val prompts = promptRepository.findByCategoryAndStage(category, stage)
        logger.info("단계 조사 시작: $category / ${stage.displayName} (${prompts.size}개)")

        return prompts.mapIndexed { index, prompt ->
            if (index > 0) {
                Thread.sleep(1000)
            }
            researchPrompt(prompt.id, variables)
        }
    }

    /**
     * 전체 4단계 조사 수행.
     */
    fun runFullResearch(request: ResearchRequest): ResearchReport {
        logger.info("전체 조사 시작: category=${request.category}, field=${request.field}")

        val variables = buildVariables(request)
        val allResults = mutableListOf<ResearchResult>()

        ResearchStage.entries.forEach { stage ->
            val stageResults = researchStage(request.category, stage, variables)
            allResults.addAll(stageResults)
            logger.info("${stage.displayName} 완료: ${stageResults.size}개 결과")
        }

        val summary = generateSummary(request, allResults)
        val recommendations = generateRecommendations(request, allResults)

        return ResearchReport(
            title = "${request.category} 유학 정보 조사: ${request.field ?: "전체"}",
            category = request.category,
            targetField = request.field,
            targetState = request.state,
            createdAt = Instant.now(),
            results = allResults,
            summary = summary,
            recommendations = recommendations
        )
    }

    private fun callLLM(userPrompt: String): String {
        val fullPrompt = "$systemPrompt\n\n$userPrompt"
        return geminiClient.generateContent(fullPrompt, temperature = 0.8, topP = 0.95)
    }

    private fun buildVariables(request: ResearchRequest): Map<String, Any> {
        return mapOf(
            "field" to (request.field ?: "일반"),
            "state" to (request.state ?: "California"),
            "cities" to (request.cities ?: "Los Angeles, San Francisco, San Diego")
        )
    }

    private fun generateSummary(request: ResearchRequest, results: List<ResearchResult>): String {
        val successCount = results.count { it.confidence > 0.5 }
        return """
            ${request.field ?: "전체"} 분야 ${request.category} 조사 완료.
            총 ${results.size}개 항목 중 ${successCount}개 성공.
            단계별 조사를 통해 학교 선정, 재정 계획, 졸업 후 로드맵을 확인했습니다.
        """.trimIndent()
    }

    @Suppress("UNUSED_PARAMETER")
    private fun generateRecommendations(
        request: ResearchRequest,
        results: List<ResearchResult>
    ): List<String> {
        return listOf(
            "STEM OPT 가능 프로그램 우선 고려",
            "취업 지원 서비스가 강한 학교 선택",
            "CPT 활용하여 재학 중 경력 쌓기",
            "H-1B 또는 EB-3 경로 미리 계획"
        )
    }
}
