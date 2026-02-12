package com.goalmond.api.domain.dto

data class ProgramResponse(
    val id: String,
    val schoolId: String,
    val schoolName: String,
    val programName: String,
    val type: String,
    val degree: String,
    val duration: String,
    val tuition: Int,
    val state: String,
    val city: String,
    val optAvailable: Boolean,
    val transferRate: Int,
    val careerPath: String
)

data class ProgramListResponse(
    val total: Int,
    val page: Int,
    val size: Int,
    val programs: List<ProgramResponse>
)
