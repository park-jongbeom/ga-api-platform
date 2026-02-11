package com.goalmond.api.service.matching

import com.goalmond.api.config.ai.CustomGeminiChatModel
import com.goalmond.api.domain.entity.AcademicProfile
import com.goalmond.api.domain.entity.Program
import com.goalmond.api.domain.entity.School
import com.goalmond.api.domain.entity.UserPreference
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

/**
 * RAG 서비스 (Spring AI 활용).
 * 
 * Spring AI의 QuestionAnswerAdvisor를 사용하여 RAG 패턴을 구현합니다.
 * 관련 문서를 자동으로 검색하고 프롬프트에 포함하여 더 정확한 설명을 생성합니다.
 * 
 * 동작 방식:
 * 1. 사용자 프롬프트 생성
 * 2. QuestionAnswerAdvisor가 VectorStore에서 관련 문서 검색
 * 3. 검색된 문서를 시스템 프롬프트에 자동 포함
 * 4. CustomGeminiChatModel로 설명 생성
 */
@Service
class RAGService(
    private val chatModel: CustomGeminiChatModel,
    @Qualifier("schoolDocumentVectorStore")
    private val schoolDocumentVectorStore: VectorStore,
    private val crawledDataContextBuilder: CrawledDataContextBuilder
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    /**
     * ChatClient를 lazy 초기화.
     * QuestionAnswerAdvisor는 빈 생성이 아닌 메서드 호출 시 생성.
     */
    private val chatClient: ChatClient by lazy {
        ChatClient.builder(chatModel).build()
    }
    
    /**
     * RAG 기반 매칭 설명 생성.
     * 
     * @param profile 학생 학력 프로필
     * @param preference 학생 선호도
     * @param program 추천 프로그램
     * @param school 추천 학교
     * @param scores 매칭 점수
     * @return 2-3문장의 설명
     */
    fun generateExplanationWithRAG(
        profile: AcademicProfile,
        preference: UserPreference,
        program: Program,
        school: School,
        scores: ScoreBreakdown
    ): String {
        return try {
            // QuestionAnswerAdvisor 생성 (문서 검색 + 프롬프트 자동 구성)
            val qaAdvisor = QuestionAnswerAdvisor(schoolDocumentVectorStore)
            
            // 사용자 프롬프트
            val userPrompt = buildUserPrompt(profile, preference, program, school, scores)
            
            logger.debug("RAG prompt: $userPrompt")
            
            // RAG 실행 (문서 검색 + 생성)
            val response = chatClient.prompt()
                .advisors(qaAdvisor)
                .user(userPrompt)
                .call()
                .content() ?: throw RAGException("Empty response from Gemini")
            
            logger.info("RAG explanation generated for school: ${school.name}")
            response
            
        } catch (e: Exception) {
            logger.error("RAG explanation failed for school: ${school.name}", e)
            throw RAGException("Failed to generate RAG explanation", e)
        }
    }
    
    /**
     * 사용자 프롬프트 생성.
     * 
     * QuestionAnswerAdvisor가 이 프롬프트를 기반으로 관련 문서를 검색하고
     * 검색된 문서를 프롬프트에 포함하여 LLM에 전달합니다.
     */
    internal fun buildUserPrompt(
        profile: AcademicProfile,
        preference: UserPreference,
        program: Program,
        school: School,
        scores: ScoreBreakdown
    ): String {
        val crawledContext = crawledDataContextBuilder.buildCrawledDataContext(school)
        val enhancedPersona = """
당신은 10년 경력의 미국 유학 전문 컨설턴트입니다.

역할:
- 학생의 프로필을 정확히 분석하여 학교 적합도를 평가
- 학교 공식 데이터(편입률, 취업률, 리뷰 등)를 인용하여 신뢰성 있는 설명 제공
- 단순 홍보가 아닌 객관적이고 균형 잡힌 정보 전달
- 유학생 관점에서 비자, OPT, 편입 등 실질적 조언

응답 원칙:
1. 검색된 문서의 구체적 데이터를 반드시 인용 (예: "편입률 65%", "취업률 82%")
2. 학생의 GPA, 예산, 목표와 학교를 명시적으로 비교
3. 긍정적 측면과 주의할 점을 균형 있게 언급
4. 2-3문장으로 간결하게 작성
        """.trimIndent()

        return """
$enhancedPersona

[학생 정보]
- GPA: ${profile.gpa} / ${profile.gpaScale}
- 영어 점수: ${profile.englishScore} (${profile.englishTestType})
- 예산: ${'$'}${preference.budgetUsd}/년
- 목표 전공: ${preference.targetMajor}
- 커리어 목표: ${preference.careerGoal}

[추천 학교]
- 이름: ${school.name}
- 위치: ${school.city}, ${school.state}
- 프로그램: ${program.name} (${program.type})
- 학비: ${'$'}${program.tuition}/년
- 합격률: ${school.acceptanceRate}%
- 편입률: ${school.transferRate}%
${if (crawledContext.isNotBlank()) crawledContext else ""}

[매칭 점수]
- 총점: ${String.format("%.1f", scores.total())}점
- 학업 적합도: ${String.format("%.1f", scores.academic)}점
- 예산 적합도: ${String.format("%.1f", scores.budget)}점
- 진로 연계성: ${String.format("%.1f", scores.career)}점
- 영어 적합도: ${String.format("%.1f", scores.english)}점
- 지역 선호: ${String.format("%.1f", scores.location)}점

[필수 출력 형식]
"이 학교는 [학생 프로필과 비교]. [학교 강점 + 데이터 인용]. [주의사항 또는 추가 장점]."

예시: "이 학교는 학생의 GPA 3.5가 평균 입학 요건(3.2)을 상회하며 예산 ${'$'}30,000 내에서 학비 ${'$'}9,000로 매우 적합합니다. UCLA 편입률 65%(2024년 기준)로 편입 목표 달성 가능성이 높습니다. 다만 경쟁이 치열하므로 좋은 성적 유지가 필요합니다."

위 정보와 검색된 문서를 바탕으로, 왜 이 학교를 추천하는지 설명해주세요.
설명은 "이 학교는..."으로 시작하고, 반드시 구체적 데이터(편입률, 학비, 합격률 등)를 인용하세요.
        """.trimIndent()
    }
}

/**
 * RAG 예외.
 */
class RAGException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
