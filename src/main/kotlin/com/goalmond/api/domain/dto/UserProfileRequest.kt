package com.goalmond.api.domain.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size

data class ProfileUpdateRequest(
    @field:Size(max = 20)
    val mbti: String? = null,
    val tags: String? = null,
    @field:Size(max = 500)
    val bio: String? = null
)

data class EducationRequest(
    @field:Size(min = 1, max = 255)
    val schoolName: String,
    @field:Size(max = 255)
    val schoolLocation: String? = null,
    @field:Min(0) @field:Max(4)
    val gpa: java.math.BigDecimal? = null,
    val englishTestType: String? = null,
    @field:Min(0) @field:Max(120)
    val englishScore: Int? = null,
    val degreeType: String? = null,
    @field:Size(min = 1, max = 255)
    val degree: String = "고등학교"
)

data class PreferenceRequest(
    val targetProgram: String? = null,
    val targetMajor: String? = null,
    val targetLocation: String? = null,
    @field:Min(0)
    val budgetUsd: Int? = null,
    val careerGoal: String? = null,
    val preferredTrack: String? = null
)
