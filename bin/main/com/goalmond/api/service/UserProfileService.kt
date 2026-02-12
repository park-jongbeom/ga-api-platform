package com.goalmond.api.service

import com.goalmond.api.domain.dto.CompleteUserProfileResponse
import com.goalmond.api.domain.dto.EducationRequest
import com.goalmond.api.domain.dto.EducationResponse
import com.goalmond.api.domain.dto.PreferenceRequest
import com.goalmond.api.domain.dto.PreferenceResponse
import com.goalmond.api.domain.dto.ProfileResponse
import com.goalmond.api.domain.dto.ProfileUpdateRequest
import com.goalmond.api.domain.entity.AcademicProfile
import com.goalmond.api.domain.entity.UserPreference
import com.goalmond.api.repository.AcademicProfileRepository
import com.goalmond.api.repository.UserPreferenceRepository
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@Profile("local", "lightsail")
class UserProfileService(
    private val userPreferenceRepository: UserPreferenceRepository,
    private val academicProfileRepository: AcademicProfileRepository
) {

    fun updateProfile(userId: UUID, request: ProfileUpdateRequest) {
        var pref = userPreferenceRepository.findByUserId(userId)
        if (pref == null) {
            pref = UserPreference(userId = userId)
        }
        request.mbti?.let { pref.mbti = it }
        request.tags?.let { pref.tags = it }
        request.bio?.let { pref.bio = it }
        userPreferenceRepository.save(pref)
    }

    fun saveEducation(userId: UUID, request: EducationRequest) {
        request.gpa?.let { gpa ->
            val scale = request.gpaScale ?: java.math.BigDecimal("4.0")
            if (gpa > scale) {
                throw IllegalArgumentException("GPA는 GPA 스케일($scale)을 초과할 수 없습니다.")
            }
        }
        request.englishScore?.let { score ->
            if (score < 0 || score > 120) {
                throw IllegalArgumentException("영어 점수는 0~120 범위여야 합니다.")
            }
        }
        var edu = academicProfileRepository.findByUserId(userId)
        if (edu == null) {
            edu = AcademicProfile(userId = userId, schoolName = request.schoolName, degree = request.degree)
        }
        edu.schoolName = request.schoolName
        edu.schoolLocation = request.schoolLocation
        edu.gpa = request.gpa
        edu.gpaScale = request.gpaScale
        edu.englishTestType = request.englishTestType
        edu.englishScore = request.englishScore
        edu.degreeType = request.degreeType
        edu.degree = request.degree
        edu.major = request.major
        edu.graduationDate = request.graduationDate
        edu.institution = request.institution
        academicProfileRepository.save(edu)
    }

    fun savePreference(userId: UUID, request: PreferenceRequest) {
        var pref = userPreferenceRepository.findByUserId(userId)
        if (pref == null) {
            pref = UserPreference(userId = userId)
        }
        request.targetProgram?.let { pref.targetProgram = it }
        request.targetMajor?.let { pref.targetMajor = it }
        request.targetLocation?.let { pref.targetLocation = it }
        request.budgetUsd?.let { pref.budgetUsd = it }
        request.careerGoal?.let { pref.careerGoal = it }
        request.preferredTrack?.let { pref.preferredTrack = it }
        userPreferenceRepository.save(pref)
    }

    fun getUserProfile(userId: UUID): CompleteUserProfileResponse {
        val pref = userPreferenceRepository.findByUserId(userId)
        val edu = academicProfileRepository.findByUserId(userId)
        return CompleteUserProfileResponse(
            profile = pref?.let { ProfileResponse(it.mbti, it.tags, it.bio) },
            education = edu?.let {
                EducationResponse(
                    schoolName = it.schoolName,
                    schoolLocation = it.schoolLocation,
                    gpa = it.gpa,
                    gpaScale = it.gpaScale,
                    englishTestType = it.englishTestType,
                    englishScore = it.englishScore,
                    degreeType = it.degreeType,
                    degree = it.degree,
                    major = it.major,
                    graduationDate = it.graduationDate,
                    institution = it.institution
                )
            },
            preference = pref?.let {
                PreferenceResponse(
                    targetProgram = it.targetProgram,
                    targetMajor = it.targetMajor,
                    targetLocation = it.targetLocation,
                    budgetUsd = it.budgetUsd,
                    careerGoal = it.careerGoal,
                    preferredTrack = it.preferredTrack
                )
            }
        )
    }
}
