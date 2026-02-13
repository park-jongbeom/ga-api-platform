package com.goalmond.api.service.matching

import com.goalmond.api.domain.dto.MatchingResponse
import com.goalmond.api.domain.entity.AcademicProfile
import com.goalmond.api.domain.entity.User
import com.goalmond.api.domain.entity.UserPreference
import com.goalmond.api.repository.AcademicProfileRepository
import com.goalmond.api.repository.GraphRagEntityRepository
import com.goalmond.api.repository.ProgramRepository
import com.goalmond.api.repository.UserPreferenceRepository
import com.goalmond.api.repository.UserRepository
import com.goalmond.api.service.graphrag.GraphSearchService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.Optional
import java.util.UUID

class MatchingEngineServiceVectorSearchFallbackTest {
    @Test
    fun `벡터 검색이 실패해도 Fallback으로 매칭 응답을 반환해야 한다`() {
        val userId = UUID.randomUUID()

        val userRepository = mockk<UserRepository>()
        val academicProfileRepository = mockk<AcademicProfileRepository>()
        val userPreferenceRepository = mockk<UserPreferenceRepository>()
        val programRepository = mockk<ProgramRepository>(relaxed = true)
        val vectorSearchService = mockk<VectorSearchService>()
        val graphSearchService = mockk<GraphSearchService>(relaxed = true)
        val graphRagEntityRepository = mockk<GraphRagEntityRepository>(relaxed = true)
        val hardFilterService = mockk<HardFilterService>(relaxed = true)
        val scoringService = mockk<ScoringService>(relaxed = true)
        val pathOptimizationService = mockk<PathOptimizationService>(relaxed = true)
        val riskPenaltyService = mockk<RiskPenaltyService>(relaxed = true)
        val explanationService = mockk<ExplanationService>(relaxed = true)
        val fallbackMatchingService = mockk<FallbackMatchingService>()

        val user = User(id = userId, email = "test@test.com", fullName = "test")
        val profile = AcademicProfile(
            id = UUID.randomUUID(),
            userId = userId,
            schoolName = "테스트고등학교",
            degree = "고등학교"
        )
        val preference = UserPreference(
            id = UUID.randomUUID(),
            userId = userId,
            targetMajor = "Computer Science",
            targetLocation = "California",
            targetProgram = "CC",
            budgetUsd = 30000,
            careerGoal = "Software Engineer",
            preferredTrack = "TRANSFER"
        )

        every { userRepository.findById(userId) } returns Optional.of(user)
        every { academicProfileRepository.findByUserId(userId) } returns profile
        every { userPreferenceRepository.findByUserId(userId) } returns preference

        every { vectorSearchService.searchSimilarSchools(any(), any(), any()) } throws
            VectorSearchException("vector failed", RuntimeException("embed failed"))

        val fallbackResult = MatchingResponse.MatchingResult(
            rank = 1,
            school = MatchingResponse.SchoolSummary(
                id = UUID.randomUUID().toString(),
                name = "Santa Monica College",
                type = "community_college",
                state = "CA",
                city = "Santa Monica",
                tuition = 12000,
                imageUrl = "https://example.com/image.png"
            ),
            program = MatchingResponse.ProgramSummary(
                id = UUID.randomUUID().toString(),
                name = "컴퓨터공학 편입 프로그램",
                degree = "AA",
                duration = "2년",
                optAvailable = true
            ),
            totalScore = 70.0,
            estimatedRoi = 0.0,
            scoreBreakdown = MatchingResponse.ScoreBreakdown(
                academic = 75,
                english = 75,
                budget = 75,
                location = 75,
                duration = 75,
                career = 75
            ),
            indicatorScores = MatchingResponse.IndicatorScores(
                academicFit = 75,
                careerOutlook = 75,
                costEfficiency = 75
            ),
            recommendationType = "safe",
            explanation = "기본 추천입니다.",
            pros = listOf("장점1", "장점2", "장점3"),
            cons = listOf("유의1", "유의2")
        )

        every { fallbackMatchingService.generateFallbackResults(profile, preference) } returns listOf(fallbackResult)

        val service = MatchingEngineService(
            userRepository = userRepository,
            academicProfileRepository = academicProfileRepository,
            userPreferenceRepository = userPreferenceRepository,
            programRepository = programRepository,
            vectorSearchService = vectorSearchService,
            graphSearchService = graphSearchService,
            graphRagEntityRepository = graphRagEntityRepository,
            hardFilterService = hardFilterService,
            scoringService = scoringService,
            pathOptimizationService = pathOptimizationService,
            riskPenaltyService = riskPenaltyService,
            explanationService = explanationService,
            fallbackMatchingService = fallbackMatchingService
        )

        val response = service.executeMatching(userId)

        assertEquals(userId.toString(), response.userId)
        assertEquals(1, response.results.size)
        assertNotNull(response.indicatorDescription)
        assertTrue(!response.indicatorDescription.isNullOrBlank())
        assertNotNull(response.nextSteps)
        assertTrue(!response.nextSteps.isNullOrEmpty())
        assertEquals("맞춤형 추천을 제공합니다.", response.message)
        assertTrue(response.createdAt.isBefore(Instant.now().plusSeconds(5)))
    }
}
