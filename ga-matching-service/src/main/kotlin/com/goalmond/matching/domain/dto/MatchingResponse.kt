package com.goalmond.matching.domain.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "매칭 결과 응답")
data class MatchingResponse(
    @field:Schema(description = "매칭 결과 ID", example = "880e8400-e29b-41d4-a716-446655440003")
    val matchingId: String,
    @field:Schema(description = "사용자 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    val userId: String,
    @field:Schema(description = "총 매칭 개수", example = "5")
    val totalMatches: Int,
    @field:Schema(description = "실행 시간(ms)", example = "2340")
    val executionTimeMs: Int,
    @field:Schema(description = "매칭 결과 목록")
    val results: List<MatchingResult>,
    @field:Schema(description = "생성 시각", example = "2024-01-01T00:00:00Z")
    val createdAt: Instant
) {
    @Schema(description = "매칭 결과 항목")
    data class MatchingResult(
        @field:Schema(description = "순위", example = "1")
        val rank: Int,
        @field:Schema(description = "학교 요약 정보")
        val school: SchoolSummary,
        @field:Schema(description = "프로그램 요약 정보")
        val program: ProgramSummary,
        @field:Schema(description = "총점", example = "87.5")
        val totalScore: Double,
        @field:Schema(description = "지표별 점수")
        val scoreBreakdown: ScoreBreakdown,
        @field:Schema(description = "추천 타입", example = "safe")
        val recommendationType: String,
        @field:Schema(description = "추천 이유", example = "예산 대비 학비가 안정적이며 영어 점수가 충분하여 추천됩니다.")
        val explanation: String,
        @field:Schema(description = "장점")
        val pros: List<String>,
        @field:Schema(description = "단점")
        val cons: List<String>
    )

    @Schema(description = "학교 요약")
    data class SchoolSummary(
        @field:Schema(description = "학교 ID", example = "school-001")
        val id: String,
        @field:Schema(description = "학교명", example = "Irvine Valley College")
        val name: String,
        @field:Schema(description = "학교 유형", example = "community_college")
        val type: String,
        @field:Schema(description = "주", example = "CA")
        val state: String,
        @field:Schema(description = "도시", example = "Irvine")
        val city: String,
        @field:Schema(description = "학비", example = "18000")
        val tuition: Int,
        @field:Schema(description = "이미지 URL", example = "https://cdn.goalmond.com/schools/ivc.jpg")
        val imageUrl: String
    )

    @Schema(description = "프로그램 요약")
    data class ProgramSummary(
        @field:Schema(description = "프로그램 ID", example = "program-001")
        val id: String,
        @field:Schema(description = "프로그램명", example = "Computer Science AA")
        val name: String,
        @field:Schema(description = "학위", example = "AA")
        val degree: String,
        @field:Schema(description = "기간", example = "2 years")
        val duration: String,
        @field:Schema(description = "OPT 가능 여부", example = "true")
        val optAvailable: Boolean
    )

    @Schema(description = "지표별 점수")
    data class ScoreBreakdown(
        @field:Schema(description = "학업 적합도", example = "18")
        val academic: Int,
        @field:Schema(description = "영어 적합도", example = "14")
        val english: Int,
        @field:Schema(description = "예산 적합도", example = "15")
        val budget: Int,
        @field:Schema(description = "지역 적합도", example = "10")
        val location: Int,
        @field:Schema(description = "기간 적합도", example = "9")
        val duration: Int,
        @field:Schema(description = "진로 적합도", example = "28")
        val career: Int
    )
}
