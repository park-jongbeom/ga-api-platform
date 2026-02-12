package com.goalmond.api.service.matching

import com.goalmond.api.config.ai.CustomGeminiChatModel
import com.goalmond.api.domain.entity.AcademicProfile
import com.goalmond.api.domain.entity.Program
import com.goalmond.api.domain.entity.School
import com.goalmond.api.domain.entity.UserPreference
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.ai.vectorstore.VectorStore
import java.math.BigDecimal
import java.util.UUID

class RAGServicePromptTest {

    private val chatModel = mockk<CustomGeminiChatModel>(relaxed = true)
    private val vectorStore = mockk<VectorStore>(relaxed = true)
    private val contextBuilder = CrawledDataContextBuilder()

    private val ragService = RAGService(
        chatModel = chatModel,
        schoolDocumentVectorStore = vectorStore,
        crawledDataContextBuilder = contextBuilder
    )

    @Test
    fun `buildUserPrompt는 크롤링 추가 정보를 포함해야 한다`() {
        val profile = AcademicProfile(
            userId = UUID.randomUUID(),
            gpa = BigDecimal("3.6"),
            gpaScale = BigDecimal("4.0"),
            englishTestType = "TOEFL",
            englishScore = 92
        )
        val preference = UserPreference(
            userId = profile.userId,
            targetMajor = "Computer Science",
            budgetUsd = 32000,
            targetLocation = "California",
            careerGoal = "Software Engineer"
        )
        val program = Program(
            schoolId = UUID.randomUUID(),
            name = "Computer Science AS",
            type = "community_college",
            degree = "AS",
            duration = "2 years",
            tuition = 14000
        )
        val school = School(
            name = "Test College",
            type = "community_college",
            city = "Irvine",
            state = "CA",
            acceptanceRate = 48,
            transferRate = 72,
            employmentRate = BigDecimal("84.2"),
            facilities = """{"dormitory": true, "dining": true}""",
            eslProgram = """{"available": true, "description": "레벨별 과정"}""",
            internationalSupport = """{"available": true, "services": ["visa support"]}"""
        )
        val scores = ScoreBreakdown(
            academic = 18.0,
            english = 16.0,
            budget = 15.0,
            location = 8.0,
            duration = 9.0,
            career = 24.0
        )

        val prompt = ragService.buildUserPrompt(profile, preference, program, school, scores)

        assertThat(prompt).contains("크롤링 기반 추가 정보")
        assertThat(prompt).contains("취업률: 84.2%")
        assertThat(prompt).contains("시설: 기숙사, 식당")
        assertThat(prompt).contains("ESL 프로그램: 제공")
        assertThat(prompt).contains("유학생 지원: visa support")
    }
}
