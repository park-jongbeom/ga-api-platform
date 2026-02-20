package com.goalmond.api.service.matching

import com.goalmond.api.domain.dto.FilterResult
import com.goalmond.api.domain.dto.MatchingResponse
import com.goalmond.api.domain.entity.AcademicProfile
import com.goalmond.api.domain.entity.GraphRagEntity
import com.goalmond.api.domain.entity.Program
import com.goalmond.api.domain.entity.School
import com.goalmond.api.domain.entity.User
import com.goalmond.api.domain.entity.UserPreference
import com.goalmond.api.domain.graphrag.EntityType
import com.goalmond.api.repository.AcademicProfileRepository
import com.goalmond.api.repository.GraphRagEntityRepository
import com.goalmond.api.repository.ProgramRepository
import com.goalmond.api.repository.UserPreferenceRepository
import com.goalmond.api.repository.UserRepository
import com.goalmond.api.service.graphrag.CareerPath
import com.goalmond.api.service.graphrag.GraphSearchService
import com.goalmond.api.service.graphrag.ProgramSearchResult
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.Collections
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
            fallbackMatchingService = fallbackMatchingService,
            hybridRankingService = HybridRankingService()
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

    @Test
    fun `Hybrid 재랭킹은 벡터와 그래프를 고정 가중치로 통합해야 한다`() {
        val userId = UUID.randomUUID()
        val schoolAId = UUID.randomUUID()
        val schoolBId = UUID.randomUUID()
        val programAId = UUID.randomUUID()
        val programBId = UUID.randomUUID()

        val userRepository = mockk<UserRepository>()
        val academicProfileRepository = mockk<AcademicProfileRepository>()
        val userPreferenceRepository = mockk<UserPreferenceRepository>()
        val programRepository = mockk<ProgramRepository>()
        val vectorSearchService = mockk<VectorSearchService>()
        val graphSearchService = mockk<GraphSearchService>()
        val graphRagEntityRepository = mockk<GraphRagEntityRepository>()
        val hardFilterService = mockk<HardFilterService>()
        val scoringService = mockk<ScoringService>()
        val pathOptimizationService = mockk<PathOptimizationService>()
        val riskPenaltyService = mockk<RiskPenaltyService>()
        val explanationService = mockk<ExplanationService>()
        val fallbackMatchingService = mockk<FallbackMatchingService>()

        val user = User(id = userId, email = "hybrid@test.com", fullName = "hybrid")
        val profile = AcademicProfile(
            id = UUID.randomUUID(),
            userId = userId,
            schoolName = "테스트고등학교",
            degree = "고등학교",
            englishTestType = "TOEFL",
            englishScore = 90
        )
        val preference = UserPreference(
            id = UUID.randomUUID(),
            userId = userId,
            targetMajor = "AI",
            targetLocation = "California",
            targetProgram = "community_college",
            budgetUsd = 50000,
            careerGoal = "AI Engineer at Google",
            preferredTrack = "TRANSFER"
        )

        val schoolA = School(id = schoolAId, name = "Vector High School", type = "community_college", state = "CA")
        val schoolB = School(id = schoolBId, name = "Graph Strong School", type = "community_college", state = "CA")
        val programA = Program(id = programAId, schoolId = schoolAId, name = "AI Program A", type = "community_college", degree = "AA")
        val programB = Program(id = programBId, schoolId = schoolBId, name = "AI Program B", type = "community_college", degree = "AA")

        every { userRepository.findById(userId) } returns Optional.of(user)
        every { academicProfileRepository.findByUserId(userId) } returns profile
        every { userPreferenceRepository.findByUserId(userId) } returns preference

        every { vectorSearchService.searchSimilarSchools(any(), any(), any()) } returns listOf(
            VectorSearchCandidate(school = schoolA, similarity = 0.95),
            VectorSearchCandidate(school = schoolB, similarity = 0.65)
        )

        every { programRepository.findBySchoolIdIn(any()) } returns listOf(programA, programB)
        every { hardFilterService.filterPrograms(any(), any(), any()) } returns FilterResult(
            passed = listOf(programA, programB),
            filtered = emptyList()
        )

        val sameScore = ScoreBreakdown(
            academic = 12.0,
            english = 10.0,
            budget = 10.0,
            location = 8.0,
            duration = 8.0,
            career = 12.0
        )
        every { scoringService.calculateScore(any(), any(), any(), any()) } returns sameScore
        every { pathOptimizationService.applyOptimization(any(), any(), any(), any()) } returns 0.0
        every { riskPenaltyService.applyPenalty(any(), any(), any(), any()) } returns 0.0
        every { explanationService.generateProsAndCons(any(), any(), any(), any(), any()) } returns Pair(
            listOf("장점1", "장점2", "장점3"),
            listOf("유의사항1")
        )

        every { graphRagEntityRepository.searchByTypeAndName(EntityType.COMPANY, any()) } answers {
            val term = secondArg<String>().lowercase()
            if (term.contains("google")) {
                listOf(GraphRagEntity(entityType = EntityType.COMPANY, entityName = "Google", canonicalName = "google"))
            } else {
                emptyList()
            }
        }
        every { graphRagEntityRepository.searchByTypeAndName(EntityType.JOB, any()) } returns emptyList()
        every { graphRagEntityRepository.searchByTypeAndName(EntityType.SKILL, any()) } answers {
            val term = secondArg<String>().lowercase()
            if (term.contains("ai")) {
                listOf(GraphRagEntity(entityType = EntityType.SKILL, entityName = "AI", canonicalName = "ai"))
            } else {
                emptyList()
            }
        }

        every { graphSearchService.findCareerPaths(any(), any(), any(), any()) } returns listOf(
            CareerPath(
                schoolId = schoolBId,
                schoolName = "Graph Strong School",
                programId = programBId,
                programName = "AI Program B",
                skills = listOf("AI"),
                job = "AI Engineer",
                company = "Google",
                weight = 1.0,
                depth = 4,
                path = listOf("Graph Strong School", "AI Program B", "AI Engineer", "Google")
            )
        )
        every { graphSearchService.findProgramsBySkills(any(), any()) } returns listOf(
            ProgramSearchResult(
                programId = programAId,
                programName = "AI Program A",
                schoolId = schoolAId,
                schoolName = "Vector High School",
                matchedSkills = listOf("AI"),
                relevanceScore = 1.0
            ),
            ProgramSearchResult(
                programId = programBId,
                programName = "AI Program B",
                schoolId = schoolBId,
                schoolName = "Graph Strong School",
                matchedSkills = listOf("AI"),
                relevanceScore = 0.2
            )
        )

        every { fallbackMatchingService.generateFallbackResults(any(), any()) } returns Collections.emptyList()

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
            fallbackMatchingService = fallbackMatchingService,
            hybridRankingService = HybridRankingService()
        )

        val response = service.executeMatching(userId)

        assertThat(response.results).hasSize(2)
        assertThat(response.results[0].school.name).isEqualTo("Graph Strong School")
        assertThat(response.results[0].totalScore).isGreaterThan(response.results[1].totalScore)
        assertThat(response.results.map { it.totalScore }).isSortedAccordingTo(compareByDescending<Double> { it })
    }
}
