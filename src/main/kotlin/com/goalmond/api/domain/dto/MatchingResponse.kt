package com.goalmond.api.domain.dto

import java.time.Instant

data class MatchingResponse(
    val matchingId: String,
    val userId: String,
    val totalMatches: Int,
    val executionTimeMs: Int,
    val results: List<MatchingResult>,
    val createdAt: Instant,
    /** DB 데이터 없을 때 Fallback(AI 추천) 사용 시 안내 문구. null이면 RAG/DB 기반 결과. */
    val message: String? = null,
    /** Hard Filter에서 필터링된 통계 (Fallback 시에만 제공). null이면 정상 매칭 결과. */
    val filterSummary: FilterSummary? = null
) {
    data class MatchingResult(
        val rank: Int,
        val school: SchoolSummary,
        val program: ProgramSummary,
        val totalScore: Double,
        val estimatedRoi: Double,
        val scoreBreakdown: ScoreBreakdown,
        /** 프론트엔드 선형 게이지용 통합 지표. score_breakdown 조합으로 계산. */
        val indicatorScores: IndicatorScores,
        val recommendationType: String,
        val explanation: String,
        val pros: List<String>,
        val cons: List<String>
    )

    data class SchoolSummary(
        val id: String,
        val name: String,
        val type: String,
        val state: String,
        val city: String,
        val tuition: Int,
        val imageUrl: String,
        val globalRanking: String? = null,
        val rankingField: String? = null,
        val averageSalary: Int? = null,
        val alumniNetworkCount: Int? = null,
        val featureBadges: List<String> = emptyList()
    )

    data class ProgramSummary(
        val id: String,
        val name: String,
        val degree: String,
        val duration: String,
        val optAvailable: Boolean
    )

    data class ScoreBreakdown(
        val academic: Int,
        val english: Int,
        val budget: Int,
        val location: Int,
        val duration: Int,
        val career: Int
    )

    /**
     * 프론트엔드 선형 게이지용 통합 지표.
     * score_breakdown의 조합으로 계산.
     */
    data class IndicatorScores(
        /** 학업 적합도: (academic + english) / 2 */
        val academicFit: Int,
        /** 진로 전망: (career + location) / 2 */
        val careerOutlook: Int,
        /** 비용 효율: (budget + duration) / 2 */
        val costEfficiency: Int
    )
    
    /**
     * Hard Filter 통계 (Fallback 시 제공).
     * 사용자에게 왜 조건에 맞는 학교가 없는지 설명하는 데 사용.
     */
    data class FilterSummary(
        /** 필터링된 총 후보 수 */
        val totalCandidates: Int,
        /** 예산 초과로 필터링된 수 */
        val filteredByBudget: Int,
        /** 영어 점수 미달로 필터링된 수 */
        val filteredByEnglish: Int,
        /** 비자 요건으로 필터링된 수 */
        val filteredByVisa: Int,
        /** 후보 중 최저 학비 (USD, null이면 학비 정보 없음) */
        val minimumTuitionFound: Int?
    )
}
