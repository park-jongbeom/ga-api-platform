package com.goalmond.api.domain.dto

import java.math.BigDecimal
import java.time.LocalDate

/**
 * 프로필 기본 정보 응답 (MBTI, 태그, 자기소개).
 */
data class ProfileResponse(
    val mbti: String?,
    val tags: String?,
    val bio: String?
)

/**
 * 학력 정보 응답.
 */
data class EducationResponse(
    val schoolName: String,
    val schoolLocation: String?,
    val gpa: BigDecimal?,
    val gpaScale: BigDecimal?,
    val englishTestType: String?,
    val englishScore: Int?,
    val degreeType: String?,
    val degree: String,
    val major: String?,
    val graduationDate: LocalDate?,
    val institution: String?
)

/**
 * 유학 목표/선호 응답.
 */
data class PreferenceResponse(
    val targetProgram: String?,
    val targetMajor: String?,
    val targetLocation: String?,
    val budgetUsd: Int?,
    val careerGoal: String?,
    val preferredTrack: String?
)

/**
 * 사용자 전체 프로필 조회 응답 (프로필 + 학력 + 선호).
 */
data class CompleteUserProfileResponse(
    val profile: ProfileResponse?,
    val education: EducationResponse?,
    val preference: PreferenceResponse?
)
