package com.goalmond.api.domain.research

import java.time.Instant

/**
 * 조사 API 요청 DTO.
 */
data class ResearchRequest(
    val category: String,
    val field: String? = null,
    val state: String? = null,
    val cities: String? = null,
    val schoolId: String? = null,
    val promptIds: List<String>? = null,
    val stage: ResearchStage? = null
)

/**
 * 단일 프롬프트 조사 결과.
 */
data class ResearchResult(
    val promptId: String,
    val stageName: String,
    val question: String,
    val answer: String,
    val confidence: Double = 0.85,
    val timestamp: Instant = Instant.now()
)

/**
 * 전체 조사 보고서 (4단계 결과 + 요약 + 추천).
 */
data class ResearchReport(
    val title: String,
    val category: String,
    val targetField: String?,
    val targetState: String?,
    val createdAt: Instant,
    val results: List<ResearchResult>,
    val summary: String,
    val recommendations: List<String>
)
