package com.goalmond.api.domain.dto

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * MatchingResponse DTO 단위 테스트.
 * 매칭 리포트 확장 필드(SchoolSummary 5개, MatchingResult estimatedRoi) 검증.
 */
class MatchingResponseTest {

    @Test
    fun `SchoolSummary에 확장 필드가 포함된다`() {
        val summary = MatchingResponse.SchoolSummary(
            id = "school-1",
            name = "UC Berkeley",
            type = "university",
            state = "CA",
            city = "Berkeley",
            tuition = 45000,
            imageUrl = "https://example.com/ucb.jpg",
            globalRanking = "#4",
            rankingField = "Computer Science",
            averageSalary = 85000,
            alumniNetworkCount = 38000,
            featureBadges = listOf("OPT STEM ELIGIBLE", "ON-CAMPUS HOUSING")
        )
        assertThat(summary.globalRanking).isEqualTo("#4")
        assertThat(summary.rankingField).isEqualTo("Computer Science")
        assertThat(summary.averageSalary).isEqualTo(85000)
        assertThat(summary.alumniNetworkCount).isEqualTo(38000)
        assertThat(summary.featureBadges).containsExactly("OPT STEM ELIGIBLE", "ON-CAMPUS HOUSING")
    }

    @Test
    fun `SchoolSummary 확장 필드가 null 또는 빈 리스트일 수 있다`() {
        val summary = MatchingResponse.SchoolSummary(
            id = "school-2",
            name = "Test CC",
            type = "community_college",
            state = "CA",
            city = "Irvine",
            tuition = 18000,
            imageUrl = ""
        )
        assertThat(summary.globalRanking).isNull()
        assertThat(summary.rankingField).isNull()
        assertThat(summary.averageSalary).isNull()
        assertThat(summary.alumniNetworkCount).isNull()
        assertThat(summary.featureBadges).isEmpty()
    }

    @Test
    fun `MatchingResult에 estimatedRoi가 포함된다`() {
        val result = MatchingResponse.MatchingResult(
            rank = 1,
            school = MatchingResponse.SchoolSummary(
                id = "s1",
                name = "School",
                type = "university",
                state = "CA",
                city = "City",
                tuition = 40000,
                imageUrl = ""
            ),
            program = MatchingResponse.ProgramSummary(
                id = "p1",
                name = "CS",
                degree = "BS",
                duration = "4 years",
                optAvailable = true
            ),
            totalScore = 88.0,
            estimatedRoi = 12.5,
            scoreBreakdown = MatchingResponse.ScoreBreakdown(
                academic = 18,
                english = 14,
                budget = 15,
                location = 8,
                duration = 8,
                career = 25
            ),
            indicatorScores = MatchingResponse.IndicatorScores(
                academicFit = 16,
                careerOutlook = 16,
                costEfficiency = 11
            ),
            recommendationType = "safe",
            explanation = "추천 설명",
            pros = listOf("장점1"),
            cons = listOf("단점1")
        )
        assertThat(result.estimatedRoi).isEqualTo(12.5)
        assertThat(result.indicatorScores.academicFit).isEqualTo(16)
        assertThat(result.indicatorScores.careerOutlook).isEqualTo(16)
        assertThat(result.indicatorScores.costEfficiency).isEqualTo(11)
    }

    @Test
    fun `MatchingResponse 전체 구조가 새 필드를 포함하여 직렬화 가능하다`() {
        val response = MatchingResponse(
            matchingId = "m-1",
            userId = "u-1",
            totalMatches = 1,
            executionTimeMs = 100,
            results = listOf(
                MatchingResponse.MatchingResult(
                    rank = 1,
                    school = MatchingResponse.SchoolSummary(
                        id = "s1",
                        name = "UC Berkeley",
                        type = "university",
                        state = "CA",
                        city = "Berkeley",
                        tuition = 45000,
                        imageUrl = "",
                        globalRanking = "#4",
                        rankingField = "Computer Science",
                        averageSalary = 85000,
                        alumniNetworkCount = 38000,
                        featureBadges = listOf("OPT STEM ELIGIBLE")
                    ),
                    program = MatchingResponse.ProgramSummary(
                        id = "p1",
                        name = "CS",
                        degree = "BS",
                        duration = "4 years",
                        optAvailable = true
                    ),
                    totalScore = 90.0,
                    estimatedRoi = 10.0,
                    scoreBreakdown = MatchingResponse.ScoreBreakdown(15, 12, 14, 8, 8, 24),
                    indicatorScores = MatchingResponse.IndicatorScores(
                        academicFit = 13,
                        careerOutlook = 16,
                        costEfficiency = 11
                    ),
                    recommendationType = "safe",
                    explanation = "설명",
                    pros = emptyList(),
                    cons = emptyList()
                )
            ),
            createdAt = Instant.now()
        )
        assertThat(response.results).hasSize(1)
        assertThat(response.results[0].school.globalRanking).isEqualTo("#4")
        assertThat(response.results[0].estimatedRoi).isEqualTo(10.0)
    }

    @Test
    fun `MatchingResponse에 indicatorDescription 필드를 포함할 수 있다`() {
        val response = MatchingResponse(
            matchingId = "m-2",
            userId = "u-2",
            totalMatches = 0,
            executionTimeMs = 120,
            results = emptyList(),
            createdAt = Instant.now(),
            indicatorDescription = "학업 적합도와 진로 전망이 높아 추천 정확도가 우수합니다."
        )

        assertThat(response.indicatorDescription).contains("학업 적합도")
    }

    @Test
    fun `MatchingResponse에 nextSteps를 포함할 수 있다`() {
        val steps = listOf(
            MatchingResponse.NextStep(
                id = 1,
                title = "영어 점수 준비",
                description = "TOEFL 또는 IELTS 최소 점수를 먼저 확보하세요.",
                priority = "urgent"
            ),
            MatchingResponse.NextStep(
                id = 2,
                title = "지원 서류 점검",
                description = "학업 성적표와 SOP를 검토하세요.",
                priority = "recommended"
            )
        )

        val response = MatchingResponse(
            matchingId = "m-3",
            userId = "u-3",
            totalMatches = 0,
            executionTimeMs = 98,
            results = emptyList(),
            createdAt = Instant.now(),
            nextSteps = steps
        )

        assertThat(response.nextSteps).hasSize(2)
        assertThat(response.nextSteps?.first()?.priority).isEqualTo("urgent")
    }
}
