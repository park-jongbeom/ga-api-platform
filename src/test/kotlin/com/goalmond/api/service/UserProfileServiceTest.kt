package com.goalmond.api.service

import com.goalmond.api.domain.dto.CompleteUserProfileResponse
import com.goalmond.api.domain.dto.EducationRequest
import com.goalmond.api.domain.dto.PreferenceRequest
import com.goalmond.api.domain.dto.ProfileUpdateRequest
import com.goalmond.api.domain.entity.AcademicProfile
import com.goalmond.api.domain.entity.UserPreference
import com.goalmond.api.repository.AcademicProfileRepository
import com.goalmond.api.repository.UserPreferenceRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
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
    fun updateProfile_existingUser_updatesSuccess() {
        val existing = UserPreference(userId = userId, mbti = "INTJ", tags = "기존", bio = "기존 소개")
        every { userPreferenceRepository.findByUserId(userId) } returns existing
        every { userPreferenceRepository.save(any()) } returns existing
        service.updateProfile(userId, ProfileUpdateRequest(mbti = "ENFP", tags = "새태그", bio = "새 소개"))
        val slot = slot<UserPreference>()
        verify(exactly = 1) { userPreferenceRepository.save(capture(slot)) }
        assertEquals("ENFP", slot.captured.mbti)
        assertEquals("새 소개", slot.captured.bio)
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
    fun saveEducation_allFields_savesSuccess() {
        val req = EducationRequest(
            schoolName = "대학교",
            schoolLocation = "서울",
            gpa = BigDecimal("3.5"),
            gpaScale = BigDecimal("4.0"),
            englishTestType = "TOEFL",
            englishScore = 100,
            degreeType = "학사",
            degree = "BACHELOR",
            major = "CS",
            graduationDate = LocalDate.of(2024, 6, 1),
            institution = "테스트대학"
        )
        every { academicProfileRepository.findByUserId(userId) } returns null
        every { academicProfileRepository.save(any()) } returns mockk()
        service.saveEducation(userId, req)
        val slot = slot<AcademicProfile>()
        verify(exactly = 1) { academicProfileRepository.save(capture(slot)) }
        assertEquals("대학교", slot.captured.schoolName)
        assertEquals(BigDecimal("3.5"), slot.captured.gpa)
        assertEquals("CS", slot.captured.major)
        assertEquals(LocalDate.of(2024, 6, 1), slot.captured.graduationDate)
        assertEquals("테스트대학", slot.captured.institution)
    }

    @Test
    fun getUserProfile_completeProfile_returnsAllSections() {
        val pref = UserPreference(
            userId = userId,
            mbti = "INTJ",
            tags = "태그",
            bio = "소개",
            targetProgram = "cc",
            targetMajor = "CS",
            budgetUsd = 50000
        )
        val edu = AcademicProfile(
            userId = userId,
            schoolName = "고등학교",
            degree = "고등학교",
            gpa = BigDecimal("3.5"),
            major = "문과"
        )
        every { userPreferenceRepository.findByUserId(userId) } returns pref
        every { academicProfileRepository.findByUserId(userId) } returns edu
        val result: CompleteUserProfileResponse = service.getUserProfile(userId)
        assertNotNull(result.profile)
        assertEquals("INTJ", result.profile?.mbti)
        assertNotNull(result.education)
        assertEquals("고등학교", result.education?.schoolName)
        assertEquals("문과", result.education?.major)
        assertNotNull(result.preference)
        assertEquals(50000, result.preference?.budgetUsd)
    }

    @Test
    fun getUserProfile_empty_returnsNullSections() {
        every { userPreferenceRepository.findByUserId(userId) } returns null
        every { academicProfileRepository.findByUserId(userId) } returns null
        val result: CompleteUserProfileResponse = service.getUserProfile(userId)
        assertNull(result.profile)
        assertNull(result.education)
        assertNull(result.preference)
    }

    @Test
    fun saveEducation_gpaExceedsGpaScale_throwsIllegalArgumentException() {
        val req = EducationRequest(
            schoolName = "고등학교",
            gpa = BigDecimal("4.5"),
            gpaScale = BigDecimal("4.0"),
            degree = "고등학교"
        )
        every { academicProfileRepository.findByUserId(userId) } returns null
        assertThrows<IllegalArgumentException> {
            service.saveEducation(userId, req)
        }
    }

    @Test
    fun saveEducation_englishScoreOutOfRange_throwsIllegalArgumentException() {
        val req = EducationRequest(
            schoolName = "고등학교",
            englishScore = 150,
            degree = "고등학교"
        )
        every { academicProfileRepository.findByUserId(userId) } returns null
        assertThrows<IllegalArgumentException> {
            service.saveEducation(userId, req)
        }
    }

    @Test
    fun savePreference_savesTargetProgram() {
        every { userPreferenceRepository.findByUserId(userId) } returns null
        every { userPreferenceRepository.save(any()) } returns mockk()
        service.savePreference(userId, PreferenceRequest(targetProgram = "cc", targetMajor = "CS", budgetUsd = 50000))
        verify(exactly = 1) { userPreferenceRepository.save(any()) }
    }
}
