package com.goalmond.api.service.matching

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.goalmond.api.domain.dto.MatchingResponse
import com.goalmond.api.domain.entity.AcademicProfile
import com.goalmond.api.domain.entity.UserPreference
import com.goalmond.api.service.ai.GeminiClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * DB에 학교/임베딩 데이터가 없을 때, 프로필·선호도만으로 Gemini를 호출해 추천 결과를 생성하는 Fallback 서비스.
 * 
 * 벡터 검색 후보가 0건일 때만 사용하며, 동일한 MatchingResponse 형식으로 반환한다.
 * 응답 message에 "DB에 데이터가 없어 API 정보만으로 생성한 추천" 안내를 담는다.
 */
@Service
class FallbackMatchingService(
    private val geminiClient: GeminiClient,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val defaultScoreBreakdown = MatchingResponse.ScoreBreakdown(
        academic = 75,
        english = 75,
        budget = 75,
        location = 75,
        duration = 75,
        career = 75
    )

    /**
     * 프로필·선호도 기반으로 Gemini가 생성한 추천 5건을 MatchingResponse.MatchingResult 목록으로 반환.
     * 파싱 실패 시 빈 목록 반환.
     */
    fun generateFallbackResults(
        profile: AcademicProfile,
        preference: UserPreference
    ): List<MatchingResponse.MatchingResult> {
        val prompt = buildPrompt(profile, preference)
        return try {
            val raw = geminiClient.generateContent(prompt)
            parseAndMap(raw)
        } catch (e: Exception) {
            logger.warn("Fallback matching failed, returning empty list", e)
            emptyList()
        }
    }

    private fun buildPrompt(profile: AcademicProfile, preference: UserPreference): String {
        val profileText = buildString {
            appendLine("학력: ${profile.schoolName}, ${profile.degree}, GPA ${profile.gpa}/${profile.gpaScale}")
            profile.englishScore?.let { appendLine("영어 점수: $it (${profile.englishTestType})") }
        }
        val prefText = buildString {
            preference.targetMajor?.let { appendLine("희망 전공: $it") }
            preference.targetProgram?.let { appendLine("희망 프로그램: $it") }
            preference.targetLocation?.let { appendLine("희망 지역: $it") }
            preference.budgetUsd?.let { appendLine("예산(USD/년): $it") }
            preference.careerGoal?.let { appendLine("커리어 목표: $it") }
        }
        return """
            다음 학생 프로필과 선호도에 맞는 미국 커뮤니티컬리지·편입 프로그램 추천 5개를 JSON 배열로만 출력하세요.
            다른 설명 없이 반드시 JSON 배열 한 개만 출력합니다.

            [학생 프로필]
            $profileText

            [선호도]
            $prefText

            [출력 형식] 반드시 아래 키만 사용한 JSON 배열 (한글 설명 가능):
            [
              {
                "school_name": "학교명",
                "school_type": "community_college",
                "state": "CA",
                "city": "도시명",
                "tuition": 12000,
                "program_name": "프로그램명",
                "degree": "Associate",
                "duration": "2년",
                "explanation": "이 학교를 추천하는 이유 한 줄",
                "pros": ["장점1", "장점2"],
                "cons": ["단점1"]
              }
            ]
        """.trimIndent()
    }

    private fun parseAndMap(raw: String): List<MatchingResponse.MatchingResult> {
        val jsonStr = extractJsonArray(raw) ?: return emptyList()
        val root = try {
            objectMapper.readTree(jsonStr)
        } catch (e: Exception) {
            logger.warn("Failed to parse fallback JSON", e)
            return emptyList()
        }
        if (!root.isArray) return emptyList()
        return root.take(5).mapIndexed { index, node ->
            val rank = index + 1
            val schoolName = node.path("school_name").asText("추천 학교 $rank")
            val schoolType = node.path("school_type").asText("community_college")
            val state = node.path("state").asText("")
            val city = node.path("city").asText("")
            val tuition = node.path("tuition").asInt(0)
            val programName = node.path("program_name").asText("편입 프로그램")
            val degree = node.path("degree").asText("Associate")
            val duration = node.path("duration").asText("2년")
            val explanation = node.path("explanation").asText("프로필에 맞춘 AI 추천입니다.")
            val pros = node.path("pros").takeIf { it.isArray }?.map { it.asText() }?.take(5) ?: listOf("AI 기반 추천")
            val cons = node.path("cons").takeIf { it.isArray }?.map { it.asText() }?.take(3) ?: emptyList()

            MatchingResponse.MatchingResult(
                rank = rank,
                school = MatchingResponse.SchoolSummary(
                    id = "fallback-$rank",
                    name = schoolName,
                    type = schoolType,
                    state = state,
                    city = city,
                    tuition = tuition,
                    imageUrl = ""
                ),
                program = MatchingResponse.ProgramSummary(
                    id = "fallback-$rank-p",
                    name = programName,
                    degree = degree,
                    duration = duration,
                    optAvailable = true
                ),
                totalScore = 70.0,
                scoreBreakdown = defaultScoreBreakdown,
                recommendationType = "strategy",
                explanation = explanation,
                pros = pros,
                cons = cons
            )
        }
    }

    /** 응답 텍스트에서 JSON 배열 부분만 추출 (마크다운 코드블록 제거). */
    private fun extractJsonArray(raw: String): String? {
        var s = raw.trim()
        if (s.startsWith("```")) {
            s = s.replaceFirst("```json", "").replaceFirst("```", "").trim()
        }
        val start = s.indexOf('[')
        val end = s.lastIndexOf(']')
        if (start < 0 || end <= start) return null
        return s.substring(start, end + 1)
    }
}
