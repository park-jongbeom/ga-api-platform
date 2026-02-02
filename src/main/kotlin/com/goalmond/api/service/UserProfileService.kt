package com.goalmond.api.service

import com.goalmond.api.domain.dto.EducationRequest
import com.goalmond.api.domain.dto.PreferenceRequest
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
        var edu = academicProfileRepository.findByUserId(userId)
        if (edu == null) {
            edu = AcademicProfile(userId = userId, schoolName = request.schoolName, degree = request.degree)
        }
        edu.schoolName = request.schoolName
        edu.schoolLocation = request.schoolLocation
        edu.gpa = request.gpa
        edu.englishTestType = request.englishTestType
        edu.englishScore = request.englishScore
        edu.degreeType = request.degreeType
        edu.degree = request.degree
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
}
