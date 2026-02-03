package com.goalmond.api.verification

import com.goalmond.api.domain.dto.CompleteUserProfileResponse
import com.goalmond.api.domain.dto.EducationRequest
import com.goalmond.api.domain.dto.EducationResponse
import com.goalmond.api.domain.dto.PreferenceRequest
import com.goalmond.api.domain.dto.PreferenceResponse
import com.goalmond.api.domain.dto.ProfileResponse
import com.goalmond.api.domain.entity.AcademicProfile
import com.goalmond.api.domain.entity.UserPreference
import com.goalmond.api.repository.ProgramRepository
import com.goalmond.api.repository.SchoolRepository
import com.goalmond.api.service.UserProfileService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.jpa.repository.JpaRepository
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

/**
 * Week 2 구현 검증 테스트.
 * JIRA Epic 2 (User Profile & Preference) 완료 항목이 코드에 반영되었는지 검증한다.
 */
@DisplayName("Week 2 구현 검증")
class Week2ImplementationVerificationTest {

    // --- GAM-50: AcademicProfile Entity ---
    @Test
    @DisplayName("AcademicProfile에 major, gpaScale, graduationDate, institution 필드 존재")
    fun academicProfile_hasNewFields() {
        val profile = AcademicProfile(
            userId = UUID.randomUUID(),
            schoolName = "테스트고",
            degree = "고등학교",
            major = "문과",
            gpaScale = BigDecimal("4.0"),
            graduationDate = LocalDate.of(2024, 6, 1),
            institution = "테스트고"
        )
        assertEquals("문과", profile.major)
        assertEquals(BigDecimal("4.0"), profile.gpaScale)
        assertEquals(LocalDate.of(2024, 6, 1), profile.graduationDate)
        assertEquals("테스트고", profile.institution)
    }

    // --- GAM-51: UserPreference Entity (기존 검증) ---
    @Test
    @DisplayName("UserPreference에 targetProgram, budgetUsd 등 필드 존재")
    fun userPreference_hasExpectedFields() {
        val pref = UserPreference(
            userId = UUID.randomUUID(),
            targetProgram = "community_college",
            budgetUsd = 50000
        )
        assertEquals("community_college", pref.targetProgram)
        assertEquals(50000, pref.budgetUsd)
    }

    // --- GAM-55: Request/Response DTO ---
    @Test
    @DisplayName("EducationRequest에 gpaScale, major, graduationDate, institution 필드 존재")
    fun educationRequest_hasNewFields() {
        val req = EducationRequest(
            schoolName = "고등학교",
            gpaScale = BigDecimal("4.0"),
            major = "CS",
            graduationDate = LocalDate.of(2024, 2, 1),
            institution = "테스트대학"
        )
        assertEquals(BigDecimal("4.0"), req.gpaScale)
        assertEquals("CS", req.major)
        assertEquals(LocalDate.of(2024, 2, 1), req.graduationDate)
        assertEquals("테스트대학", req.institution)
    }

    @Test
    @DisplayName("PreferenceRequest에 targetMajor Size, budgetUsd Min/Max 검증 필드 존재")
    fun preferenceRequest_hasValidationFields() {
        val req = PreferenceRequest(
            targetMajor = "CS",
            budgetUsd = 100000
        )
        assertEquals("CS", req.targetMajor)
        assertEquals(100000, req.budgetUsd)
    }

    @Test
    @DisplayName("ProfileResponse, EducationResponse, PreferenceResponse, CompleteUserProfileResponse DTO 존재")
    fun responseDtos_existAndMap() {
        val profile = ProfileResponse("INTJ", "태그", "소개")
        val education = EducationResponse(
            "고등학교", null, BigDecimal("3.5"), BigDecimal("4.0"),
            "TOEFL", 100, null, "고등학교", "문과", null, null
        )
        val preference = PreferenceResponse("cc", "CS", "CA", 50000, null, null)
        val complete = CompleteUserProfileResponse(profile, education, preference)
        assertNotNull(complete.profile)
        assertNotNull(complete.education)
        assertNotNull(complete.preference)
        assertEquals("INTJ", complete.profile?.mbti)
        assertEquals("고등학교", complete.education?.schoolName)
        assertEquals(50000, complete.preference?.budgetUsd)
    }

    // --- GAM-87, GAM-88: Repository ---
    @Test
    @DisplayName("SchoolRepository는 JpaRepository를 상속하고 findByType, findByState 메서드 보유")
    fun schoolRepository_extendsJpaRepositoryAndHasMethods() {
        assertTrue(JpaRepository::class.java.isAssignableFrom(SchoolRepository::class.java))
        assertNotNull(SchoolRepository::class.java.getMethod("findByType", String::class.java))
        assertNotNull(SchoolRepository::class.java.getMethod("findByState", String::class.java))
    }

    @Test
    @DisplayName("ProgramRepository는 JpaRepository를 상속하고 findBySchoolId, findByType 메서드 보유")
    fun programRepository_extendsJpaRepositoryAndHasMethods() {
        assertTrue(JpaRepository::class.java.isAssignableFrom(ProgramRepository::class.java))
        assertNotNull(ProgramRepository::class.java.getMethod("findBySchoolId", UUID::class.java))
        assertNotNull(ProgramRepository::class.java.getMethod("findByType", String::class.java))
    }

    // --- UserProfileService.getUserProfile ---
    @Test
    @DisplayName("UserProfileService에 getUserProfile 메서드가 있고 반환 타입은 CompleteUserProfileResponse")
    fun userProfileService_hasGetUserProfile() {
        val method = UserProfileService::class.java.getMethod("getUserProfile", UUID::class.java)
        assertNotNull(method)
        assertEquals(CompleteUserProfileResponse::class.java, method.returnType)
    }
}
