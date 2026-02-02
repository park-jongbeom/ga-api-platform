package com.goalmond.api.service

import com.goalmond.api.domain.dto.EducationRequest
import com.goalmond.api.domain.dto.PreferenceRequest
import com.goalmond.api.domain.dto.ProfileUpdateRequest
import com.goalmond.api.domain.entity.AcademicProfile
import com.goalmond.api.domain.entity.UserPreference
import com.goalmond.api.repository.AcademicProfileRepository
import com.goalmond.api.repository.UserPreferenceRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

class UserProfileServiceTest {
    private val userPreferenceRepository = mockk<UserPreferenceRepository>()
    private val academicProfileRepository = mockk<AcademicProfileRepository>()
    private val service = UserProfileService(userPreferenceRepository, academicProfileRepository)
    private val userId = UUID.randomUUID()

    @Test
    fun updateProfile_newUser_createsUserPreference() {
        every { userPreferenceRepository.findByUserId(userId) } returns null
        every { userPreferenceRepository.save(any()) } returns mockk()
        service.updateProfile(userId, ProfileUpdateRequest(mbti = "INTJ", tags = "태그", bio = "소개"))
        verify(exactly = 1) { userPreferenceRepository.save(any()) }
    }

    @Test
    fun saveEducation_newUser_createsAcademicProfile() {
        val req = EducationRequest(schoolName = "고등학교", gpa = BigDecimal("3.5"), degree = "고등학교")
        every { academicProfileRepository.findByUserId(userId) } returns null
        every { academicProfileRepository.save(any()) } returns mockk()
        service.saveEducation(userId, req)
        verify(exactly = 1) { academicProfileRepository.save(any()) }
    }

    @Test
    fun savePreference_savesTargetProgram() {
        every { userPreferenceRepository.findByUserId(userId) } returns null
        every { userPreferenceRepository.save(any()) } returns mockk()
        service.savePreference(userId, PreferenceRequest(targetProgram = "cc", targetMajor = "CS", budgetUsd = 50000))
        verify(exactly = 1) { userPreferenceRepository.save(any()) }
    }
}
