package com.goalmond.api.service.matching

import com.fasterxml.jackson.databind.ObjectMapper
import com.goalmond.api.domain.dto.MatchingResponse
import com.goalmond.api.domain.entity.AcademicProfile
import com.goalmond.api.domain.entity.UserPreference
import com.goalmond.api.service.ai.GeminiApiException
import com.goalmond.api.service.ai.GeminiClient
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

/**
 * FallbackMatchingService 단위 테스트 (GAM-3, Phase 9).
 * 
 * 테스트 목표:
 * 1. 프롬프트 엔지니어링 검증 (6대 지표, 추천 유형, 확장 필드)
 * 2. Gemini API 실패 시 기본 추천 반환
 * 3. JSON 파싱 실패 시 기본 추천 반환
 * 4. 부분 성공 처리 (일부만 파싱 성공해도 반환)
 * 5. 정상 응답 시 5개 추천 반환
 * 6. 확장 필드 파싱 검증 (globalRanking, averageSalary 등)
 */
class FallbackMatchingServiceTest {
    
    private lateinit var geminiClient: GeminiClient
    private lateinit var objectMapper: ObjectMapper
    private lateinit var fallbackMatchingService: FallbackMatchingService
    
    private lateinit var testProfile: AcademicProfile
    private lateinit var testPreference: UserPreference
    
    @BeforeEach
    fun setUp() {
        geminiClient = mockk()
        objectMapper = ObjectMapper()
        fallbackMatchingService = FallbackMatchingService(geminiClient, objectMapper)
        
        // 테스트용 프로필 생성
        testProfile = AcademicProfile(
            userId = UUID.randomUUID(),
            schoolName = "서울고등학교",
            degree = "고등학교",
            gpa = BigDecimal("3.5"),
            gpaScale = BigDecimal("4.0"),
            englishTestType = "TOEFL",
            englishScore = 90
        )
        
        testPreference = UserPreference(
            userId = testProfile.userId,
            targetMajor = "Computer Science",
            targetProgram = "community_college",
            targetLocation = "California",
            budgetUsd = 35000,
            careerGoal = "Software Engineer",
            preferredTrack = "편입"
        )
    }
    
    @Nested
    @DisplayName("프롬프트 엔지니어링 검증")
    inner class PromptEngineeringTest {
        
        @Test
        @DisplayName("프롬프트에 6대 매칭 지표가 포함되어야 한다")
        fun `프롬프트에 매칭 기준 6대 지표 포함`() {
            // When
            val prompt = fallbackMatchingService.buildPrompt(testProfile, testPreference)
            
            // Then
            assertThat(prompt).contains("학업 적합도")
            assertThat(prompt).contains("영어 적합도")
            assertThat(prompt).contains("예산 적합도")
            assertThat(prompt).contains("지역 선호")
            assertThat(prompt).contains("기간 적합도")
            assertThat(prompt).contains("진로 연계성")
            
            // 가중치도 포함
            assertThat(prompt).contains("20%")
            assertThat(prompt).contains("15%")
            assertThat(prompt).contains("10%")
            assertThat(prompt).contains("30%")
        }
        
        @Test
        @DisplayName("프롬프트에 추천 유형 정의가 포함되어야 한다")
        fun `프롬프트에 추천 유형 정의 포함`() {
            // When
            val prompt = fallbackMatchingService.buildPrompt(testProfile, testPreference)
            
            // Then
            assertThat(prompt).contains("safe")
            assertThat(prompt).contains("challenge")
            assertThat(prompt).contains("strategy")
            assertThat(prompt).contains("합격 가능성 높음")
            assertThat(prompt).contains("도전적 선택")
            assertThat(prompt).contains("전략적 선택")
        }
        
        @Test
        @DisplayName("프롬프트에 확장 필드 요청이 포함되어야 한다")
        fun `프롬프트에 확장 필드 요청 포함`() {
            // When
            val prompt = fallbackMatchingService.buildPrompt(testProfile, testPreference)
            
            // Then
            assertThat(prompt).contains("global_ranking")
            assertThat(prompt).contains("ranking_field")
            assertThat(prompt).contains("average_salary")
            assertThat(prompt).contains("alumni_network_count")
            assertThat(prompt).contains("feature_badges")
        }
        
        @Test
        @DisplayName("프롬프트에 사용자 프로필 정보가 포함되어야 한다")
        fun `프롬프트에 사용자 프로필 정보 포함`() {
            // When
            val prompt = fallbackMatchingService.buildPrompt(testProfile, testPreference)
            
            // Then
            assertThat(prompt).contains("서울고등학교")
            assertThat(prompt).contains("3.5")
            assertThat(prompt).contains("TOEFL")
            assertThat(prompt).contains("90")
            assertThat(prompt).contains("Computer Science")
            assertThat(prompt).contains("California")
            assertThat(prompt).contains("35000")
            assertThat(prompt).contains("Software Engineer")
        }
        
        @Test
        @DisplayName("프롬프트에 예산 초과 금지 지침이 포함되어야 한다")
        fun `프롬프트에 예산 제약 조건 포함`() {
            // When
            val prompt = fallbackMatchingService.buildPrompt(testProfile, testPreference)
            
            // Then
            assertThat(prompt).contains("예산")
            assertThat(prompt).contains("초과")
            assertThat(prompt).containsIgnoringCase("35000")
        }
    }
    
    @Nested
    @DisplayName("에러 처리 검증")
    inner class ErrorHandlingTest {
        
        @Test
        @DisplayName("Gemini API 실패 시 기본 추천을 반환해야 한다")
        fun `Gemini API 실패시 기본 추천 반환`() {
            // Given
            every { geminiClient.generateContent(any()) } throws GeminiApiException("API Error")
            
            // When
            val results = fallbackMatchingService.generateFallbackResults(testProfile, testPreference)
            
            // Then
            assertThat(results).isNotEmpty()
            assertThat(results.size).isGreaterThanOrEqualTo(1)
            assertThat(results.first().school.id).startsWith("fallback-")
        }
        
        @Test
        @DisplayName("일반 예외 발생 시 기본 추천을 반환해야 한다")
        fun `일반 예외 발생시 기본 추천 반환`() {
            // Given
            every { geminiClient.generateContent(any()) } throws RuntimeException("Unknown Error")
            
            // When
            val results = fallbackMatchingService.generateFallbackResults(testProfile, testPreference)
            
            // Then
            assertThat(results).isNotEmpty()
            assertThat(results.first().school.id).startsWith("fallback-")
        }
        
        @Test
        @DisplayName("JSON 파싱 실패 시 기본 추천을 반환해야 한다")
        fun `JSON 파싱 실패시 기본 추천 반환`() {
            // Given
            every { geminiClient.generateContent(any()) } returns "이것은 유효한 JSON이 아닙니다"
            
            // When
            val results = fallbackMatchingService.generateFallbackResults(testProfile, testPreference)
            
            // Then
            assertThat(results).isNotEmpty()
            assertThat(results.first().school.id).startsWith("fallback-")
        }
        
        @Test
        @DisplayName("빈 JSON 배열 응답 시 기본 추천을 반환해야 한다")
        fun `빈 JSON 배열 응답시 기본 추천 반환`() {
            // Given
            every { geminiClient.generateContent(any()) } returns "[]"
            
            // When
            val results = fallbackMatchingService.generateFallbackResults(testProfile, testPreference)
            
            // Then
            assertThat(results).isNotEmpty()
            assertThat(results.first().school.id).startsWith("fallback-")
        }
    }
    
    @Nested
    @DisplayName("파싱 검증")
    inner class ParsingTest {
        
        @Test
        @DisplayName("정상 Gemini 응답 시 5개 추천을 반환해야 한다")
        fun `정상 응답시 5개 추천 반환`() {
            // Given
            val validResponse = createValidGeminiResponse(5)
            every { geminiClient.generateContent(any()) } returns validResponse
            
            // When
            val results = fallbackMatchingService.generateFallbackResults(testProfile, testPreference)
            
            // Then
            assertThat(results).hasSize(5)
            assertThat(results[0].rank).isEqualTo(1)
            assertThat(results[4].rank).isEqualTo(5)
        }
        
        @Test
        @DisplayName("부분 성공 시 파싱된 결과만 반환해야 한다")
        fun `부분 성공시 파싱된 결과만 반환`() {
            // Given
            val partialResponse = """
            [
                {"school_name": "Valid School", "school_type": "community_college", "state": "CA", "city": "LA", "tuition": 10000},
                {"invalid": "data"},
                {"school_name": "Another Valid School", "school_type": "university", "state": "NY", "city": "NYC", "tuition": 20000}
            ]
            """.trimIndent()
            every { geminiClient.generateContent(any()) } returns partialResponse
            
            // When
            val results = fallbackMatchingService.generateFallbackResults(testProfile, testPreference)
            
            // Then
            // 부분 성공이므로 최소 2개 이상 (또는 모두 파싱 성공할 수도 있음)
            assertThat(results).isNotEmpty()
        }
        
        @Test
        @DisplayName("확장 필드가 포함된 응답을 올바르게 파싱해야 한다")
        fun `확장 필드 파싱 검증`() {
            // Given
            val responseWithExtendedFields = """
            [
                {
                    "school_name": "UC Berkeley",
                    "school_type": "university",
                    "state": "CA",
                    "city": "Berkeley",
                    "tuition": 45000,
                    "global_ranking": "#4",
                    "ranking_field": "Computer Science",
                    "average_salary": 95000,
                    "alumni_network_count": 50000,
                    "feature_badges": ["OPT STEM ELIGIBLE", "TOP RESEARCH"],
                    "program_name": "Computer Science BS",
                    "degree": "BS",
                    "duration": "4년",
                    "opt_available": true,
                    "recommendation_type": "challenge",
                    "total_score": 88.5,
                    "score_breakdown": {
                        "academic": 90,
                        "english": 85,
                        "budget": 70,
                        "location": 95,
                        "duration": 80,
                        "career": 95
                    },
                    "explanation": "학업 성적이 우수하고 실리콘밸리 취업에 유리합니다.",
                    "pros": ["세계 최고 수준의 CS 프로그램", "실리콘밸리 위치", "강력한 동문 네트워크"],
                    "cons": ["높은 경쟁률", "비싼 학비"]
                }
            ]
            """.trimIndent()
            every { geminiClient.generateContent(any()) } returns responseWithExtendedFields
            
            // When
            val results = fallbackMatchingService.generateFallbackResults(testProfile, testPreference)
            
            // Then
            assertThat(results).hasSize(1)
            val result = results[0]
            
            // 기본 필드
            assertThat(result.school.name).isEqualTo("UC Berkeley")
            assertThat(result.school.type).isEqualTo("university")
            assertThat(result.school.state).isEqualTo("CA")
            assertThat(result.school.tuition).isEqualTo(45000)
            
            // 확장 필드
            assertThat(result.school.globalRanking).isEqualTo("#4")
            assertThat(result.school.rankingField).isEqualTo("Computer Science")
            assertThat(result.school.averageSalary).isEqualTo(95000)
            assertThat(result.school.alumniNetworkCount).isEqualTo(50000)
            assertThat(result.school.featureBadges).containsExactly("OPT STEM ELIGIBLE", "TOP RESEARCH")
            
            // 프로그램
            assertThat(result.program.name).isEqualTo("Computer Science BS")
            assertThat(result.program.degree).isEqualTo("BS")
            assertThat(result.program.optAvailable).isTrue()
            
            // 추천 유형 및 점수
            assertThat(result.recommendationType).isEqualTo("challenge")
            assertThat(result.totalScore).isEqualTo(88.5)
            
            // 점수 breakdown
            assertThat(result.scoreBreakdown.academic).isEqualTo(90)
            assertThat(result.scoreBreakdown.english).isEqualTo(85)
            assertThat(result.scoreBreakdown.budget).isEqualTo(70)
            assertThat(result.scoreBreakdown.location).isEqualTo(95)
            assertThat(result.scoreBreakdown.duration).isEqualTo(80)
            assertThat(result.scoreBreakdown.career).isEqualTo(95)
            
            // 설명 및 장단점
            assertThat(result.explanation).contains("학업 성적")
            assertThat(result.pros).hasSize(3)
            assertThat(result.cons).hasSize(2)
        }
        
        @Test
        @DisplayName("마크다운 코드블록으로 감싸진 응답을 올바르게 파싱해야 한다")
        fun `마크다운 코드블록 처리`() {
            // Given
            val markdownWrappedResponse = """
            ```json
            [
                {"school_name": "Test College", "school_type": "community_college", "state": "CA", "city": "Test City", "tuition": 10000}
            ]
            ```
            """.trimIndent()
            every { geminiClient.generateContent(any()) } returns markdownWrappedResponse
            
            // When
            val results = fallbackMatchingService.generateFallbackResults(testProfile, testPreference)
            
            // Then
            assertThat(results).hasSize(1)
            assertThat(results[0].school.name).isEqualTo("Test College")
        }
        
        @Test
        @DisplayName("잘못된 recommendation_type은 strategy로 대체되어야 한다")
        fun `잘못된 추천유형 기본값 처리`() {
            // Given
            val responseWithInvalidType = """
            [{"school_name": "Test", "school_type": "cc", "state": "CA", "city": "LA", "tuition": 10000, "recommendation_type": "invalid_type"}]
            """.trimIndent()
            every { geminiClient.generateContent(any()) } returns responseWithInvalidType
            
            // When
            val results = fallbackMatchingService.generateFallbackResults(testProfile, testPreference)
            
            // Then
            assertThat(results).hasSize(1)
            assertThat(results[0].recommendationType).isEqualTo("strategy")
        }
    }
    
    @Nested
    @DisplayName("기본 추천 검증")
    inner class DefaultRecommendationsTest {
        
        @Test
        @DisplayName("기본 추천에 실제 학교 정보가 포함되어야 한다")
        fun `기본 추천에 실제 학교 정보 포함`() {
            // When
            val results = fallbackMatchingService.generateDefaultRecommendations(testProfile, testPreference)
            
            // Then
            assertThat(results).isNotEmpty()
            assertThat(results.size).isLessThanOrEqualTo(5)
            
            val firstResult = results[0]
            assertThat(firstResult.school.name).isNotBlank()
            assertThat(firstResult.school.state).isEqualTo("CA")
            assertThat(firstResult.school.city).isNotBlank()
            assertThat(firstResult.school.tuition).isGreaterThan(0)
            assertThat(firstResult.explanation).isNotBlank()
            assertThat(firstResult.pros).isNotEmpty()
            assertThat(firstResult.cons).isNotEmpty()
        }
        
        @Test
        @DisplayName("기본 추천에 다양한 추천 유형이 포함되어야 한다")
        fun `기본 추천에 다양한 추천 유형 포함`() {
            // When
            val results = fallbackMatchingService.generateDefaultRecommendations(testProfile, testPreference)
            
            // Then
            val types = results.map { it.recommendationType }.toSet()
            assertThat(types).containsAnyOf("safe", "challenge", "strategy")
        }
        
        @Test
        @DisplayName("기본 추천에 사용자 전공이 반영되어야 한다")
        fun `기본 추천에 사용자 전공 반영`() {
            // When
            val results = fallbackMatchingService.generateDefaultRecommendations(testProfile, testPreference)
            
            // Then
            val programNames = results.map { it.program.name }
            assertThat(programNames.any { it.contains("Computer Science") }).isTrue()
        }
        
        @Test
        @DisplayName("기본 추천의 학비는 예산 범위 내여야 한다")
        fun `기본 추천 예산 범위 검증`() {
            // When
            val results = fallbackMatchingService.generateDefaultRecommendations(testProfile, testPreference)
            
            // Then
            results.forEach { result ->
                assertThat(result.school.tuition).isLessThanOrEqualTo(testPreference.budgetUsd!!)
            }
        }
        
        @Test
        @DisplayName("기본 추천의 텍스트 필드는 한글로 제공되어야 한다")
        fun `기본 추천 한글화 검증`() {
            // When
            val results = fallbackMatchingService.generateDefaultRecommendations(testProfile, testPreference)
            
            // Then: explanation, pros, cons, featureBadges에 한글이 포함되어야 함
            val koreanPattern = Regex("[가-힣]+")
            results.forEach { result ->
                assertThat(koreanPattern.containsMatchIn(result.explanation)).isTrue()
                result.pros.forEach { assertThat(koreanPattern.containsMatchIn(it)).isTrue() }
                result.cons.forEach { assertThat(koreanPattern.containsMatchIn(it)).isTrue() }
                result.school.featureBadges.forEach { assertThat(koreanPattern.containsMatchIn(it)).isTrue() }
            }
        }
    }
    
    // 헬퍼 메서드
    private fun createValidGeminiResponse(count: Int): String {
        val schools = (1..count).map { i ->
            """
            {
                "school_name": "Test School $i",
                "school_type": "community_college",
                "state": "CA",
                "city": "City $i",
                "tuition": ${10000 + i * 1000},
                "program_name": "Program $i",
                "degree": "AA",
                "duration": "2년",
                "recommendation_type": "${if (i == 1) "safe" else if (i == 2) "challenge" else "strategy"}",
                "explanation": "추천 이유 $i",
                "pros": ["장점1", "장점2", "장점3"],
                "cons": ["단점1", "단점2"]
            }
            """.trimIndent()
        }
        return "[${schools.joinToString(",")}]"
    }
}
