package com.goalmond.api.domain.dto

import java.time.Instant

data class MatchingResponse(
    val matchingId: String,
    val userId: String,
    val totalMatches: Int,
    val executionTimeMs: Int,
    val results: List<MatchingResult>,
    val createdAt: Instant
) {
    data class MatchingResult(
        val rank: Int,
        val school: SchoolSummary,
        val program: ProgramSummary,
        val totalScore: Double,
        val scoreBreakdown: ScoreBreakdown,
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
        val imageUrl: String
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
}
