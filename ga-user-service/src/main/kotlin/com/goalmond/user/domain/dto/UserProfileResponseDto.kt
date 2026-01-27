package com.goalmond.user.domain.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "사용자 상세 프로필 응답")
data class UserProfileResponseDto(
    @field:Schema(description = "사용자 UUID", example = "123e4567-e89b-12d3-a456-426614174000")
    val userId: String,
    @field:Schema(description = "이메일", example = "user@example.com")
    val email: String,
    @field:Schema(description = "이름", example = "홍길동")
    val name: String,
    @field:Schema(description = "학업 프로필 목록")
    val academicProfiles: List<AcademicProfileDto>,
    @field:Schema(description = "재정 프로필 목록")
    val financialProfiles: List<FinancialProfileDto>,
    @field:Schema(description = "사용자 선호도 목록")
    val preferences: List<PreferenceDto>
) {
    @Schema(description = "학업 프로필")
    data class AcademicProfileDto(
        @field:Schema(description = "학업 프로필 ID", example = "academic-1")
        val id: String,
        @field:Schema(description = "학위", example = "Bachelor")
        val degree: String,
        @field:Schema(description = "전공", example = "Computer Science")
        val major: String,
        @field:Schema(description = "GPA", example = "3.8")
        val gpa: Double,
        @field:Schema(description = "기관", example = "Seoul University")
        val institution: String
    )

    @Schema(description = "재정 프로필")
    data class FinancialProfileDto(
        @field:Schema(description = "재정 프로필 ID", example = "financial-1")
        val id: String,
        @field:Schema(description = "예산 범위", example = "10000-20000")
        val budgetRange: String,
        @field:Schema(description = "자금 출처", example = "Personal")
        val fundingSource: String
    )

    @Schema(description = "사용자 선호도")
    data class PreferenceDto(
        @field:Schema(description = "선호도 ID", example = "pref-1")
        val id: String,
        @field:Schema(description = "선호 전공", example = "Engineering")
        val preferredMajor: String,
        @field:Schema(description = "진로 트랙", example = "Software Developer")
        val careerTrack: String
    )
}
