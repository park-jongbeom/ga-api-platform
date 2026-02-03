package com.goalmond.api.domain.dto

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
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
    @field:DecimalMin("0.0") @field:DecimalMax("5.0")
    val gpa: java.math.BigDecimal? = null,
    @field:DecimalMin("1.0") @field:DecimalMax("5.0")
    val gpaScale: java.math.BigDecimal? = java.math.BigDecimal("4.0"),
    val englishTestType: String? = null,
    @field:Min(0) @field:Max(120)
    val englishScore: Int? = null,
    val degreeType: String? = null,
    @field:Size(min = 1, max = 255)
    val degree: String = "고등학교",
    @field:Size(max = 100)
    val major: String? = null,
    val graduationDate: java.time.LocalDate? = null,
    @field:Size(max = 255)
    val institution: String? = null
)

data class PreferenceRequest(
    val targetProgram: String? = null,
    @field:Size(max = 100)
    val targetMajor: String? = null,
    val targetLocation: String? = null,
    @field:Min(0) @field:Max(500000)
    val budgetUsd: Int? = null,
    @field:Size(max = 1000)
    val careerGoal: String? = null,
    val preferredTrack: String? = null
)
