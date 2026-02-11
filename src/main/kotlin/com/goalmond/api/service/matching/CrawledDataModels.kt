package com.goalmond.api.service.matching

/**
 * schools JSONB 컬럼 파싱 모델.
 */
data class FacilityInfo(
    val dormitory: Boolean = false,
    val dining: Boolean = false,
    val gym: Boolean = false,
    val library: Boolean = false,
    val lab: Boolean = false,
    val entertainment: Boolean = false
)

data class EslProgramInfo(
    val available: Boolean = false,
    val description: String? = null
)

data class InternationalSupportInfo(
    val available: Boolean = false,
    val services: List<String> = emptyList(),
    val description: String? = null
)
