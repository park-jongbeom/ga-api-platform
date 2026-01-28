package com.goalmond.user.domain.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDate

@Schema(description = "사용자 상세 프로필 응답")
data class UserProfileResponseDto(
    @field:Schema(description = "사용자 UUID", example = "123e4567-e89b-12d3-a456-426614174000")
    val userId: String,
    @field:Schema(description = "이메일", example = "user@example.com")
    val email: String,
    @field:Schema(description = "전체 이름", example = "홍길동")
    val fullName: String,
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
        @field:Schema(description = "학교명", example = "Seoul University")
        val schoolName: String,
        @field:Schema(description = "학위 유형", example = "Bachelor")
        val degreeType: String?,
        @field:Schema(description = "학위", example = "BACHELOR")
        val degree: String,
        @field:Schema(description = "전공", example = "Computer Science")
        val major: String?,
        @field:Schema(description = "GPA", example = "3.8")
        val gpa: BigDecimal?,
        @field:Schema(description = "GPA 스케일", example = "4.0")
        val gpaScale: BigDecimal?,
        @field:Schema(description = "졸업일", example = "2024-06-15")
        val graduationDate: LocalDate?,
        @field:Schema(description = "기관", example = "Seoul University")
        val institution: String?
    )

    @Schema(description = "재정 프로필")
    data class FinancialProfileDto(
        @field:Schema(description = "재정 프로필 ID", example = "financial-1")
        val id: String,
        @field:Schema(description = "예산 범위", example = "10000-20000")
        val budgetRange: String,
        @field:Schema(description = "총 예산 (USD)", example = "50000")
        val totalBudgetUsd: Int?,
        @field:Schema(description = "등록금 한도 (USD)", example = "30000")
        val tuitionLimitUsd: Int?,
        @field:Schema(description = "자금 출처", example = "Personal")
        val fundingSource: String?
    )

    @Schema(description = "사용자 선호도")
    data class PreferenceDto(
        @field:Schema(description = "선호도 ID", example = "pref-1")
        val id: String,
        @field:Schema(description = "목표 전공", example = "Engineering")
        val targetMajor: String?,
        @field:Schema(description = "목표 지역", example = "Seoul")
        val targetLocation: String?,
        @field:Schema(description = "진로 목표", example = "Software Developer")
        val careerGoal: String?,
        @field:Schema(description = "선호 트랙", example = "Software Developer")
        val preferredTrack: String?
    )
}
