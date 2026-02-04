package com.goalmond.api.service.research

import com.goalmond.api.domain.research.ResearchStage
import com.goalmond.api.service.ai.GeminiApiException
import com.goalmond.api.service.ai.GeminiClient
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * ResearchAgentService 단위 테스트.
 * 프롬프트 렌더링, 단일 실행, Vocational 10개 로드 검증.
 */
class ResearchAgentServiceTest {

    private lateinit var geminiClient: GeminiClient
    private lateinit var promptRepository: PromptRepository
    private lateinit var researchAgentService: ResearchAgentService

    @BeforeEach
    fun setUp() {
        geminiClient = mockk()
        promptRepository = PromptRepository()
        promptRepository.initialize()
        researchAgentService = ResearchAgentService(geminiClient, promptRepository)
    }

    @Test
    @DisplayName("프롬프트 렌더링이 정확히 동작해야 한다")
    fun `프롬프트 렌더링이 정확히 동작해야 한다`() {
        val template = promptRepository.findById("P1_COLLEGE_TYPES")
        assertThat(template).isNotNull

        val rendered = template!!.render(mapOf("field" to "IT"))

        assertThat(rendered).contains("Community College")
        assertThat(rendered).contains("Trade School")
        assertThat(rendered).contains("한국어")
    }

    @Test
    @DisplayName("단일 프롬프트 실행 시 한글 응답이 반환되어야 한다")
    fun `단일 프롬프트 실행 시 한글 응답이 반환되어야 한다`() {
        every { geminiClient.generateContent(any()) } returns "Community College는 2년제이며, Trade School는 1년..."

        val result = researchAgentService.researchPrompt(
            "P1_COLLEGE_TYPES",
            mapOf("field" to "IT")
        )

        assertThat(result.answer).isNotBlank
        assertThat(result.promptId).isEqualTo("P1_COLLEGE_TYPES")
        assertThat(result.stageName).isEqualTo("기본 구조 파악")
    }

    @Test
    @DisplayName("Vocational 프롬프트가 10개 로드되어야 한다")
    fun `Vocational 프롬프트가 10개 로드되어야 한다`() {
        val vocationalPrompts = promptRepository.findByCategory("vocational")

        assertThat(vocationalPrompts).hasSize(10)
        assertThat(vocationalPrompts.map { it.id }).containsExactlyInAnyOrder(
            "P1_COLLEGE_TYPES",
            "P2_TOP_PROGRAMS",
            "P3_SCHOOL_LIST",
            "P4_STEM_PROGRAMS",
            "P5_CAREER_SERVICES",
            "P6_SCHOLARSHIPS",
            "P7_CPT_WORK",
            "P8_H1B_PATH",
            "P9_GREEN_CARD",
            "P10_CITY_ANALYSIS"
        )
    }

    @Test
    @DisplayName("변수 치환이 있는 프롬프트 렌더링")
    fun `변수 치환이 있는 프롬프트 렌더링`() {
        val template = promptRepository.findById("P2_TOP_PROGRAMS")
        assertThat(template).isNotNull

        val rendered = template!!.render(mapOf("field" to "기계"))

        assertThat(rendered).contains("기계")
        assertThat(rendered).contains("한국어")
    }

    @Test
    @DisplayName("존재하지 않는 프롬프트 ID 시 예외")
    fun `존재하지 않는 프롬프트 ID 시 예외`() {
        org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            researchAgentService.researchPrompt("INVALID_ID", emptyMap())
        }
    }

    @Test
    @DisplayName("Gemini API 실패 시 confidence 0으로 결과 반환")
    fun `Gemini API 실패 시 confidence 0으로 결과 반환`() {
        every { geminiClient.generateContent(any()) } throws GeminiApiException("API Error")

        val result = researchAgentService.researchPrompt(
            "P1_COLLEGE_TYPES",
            emptyMap()
        )

        assertThat(result.confidence).isEqualTo(0.0)
        assertThat(result.answer).contains("AI 응답 생성 실패")
    }
}
