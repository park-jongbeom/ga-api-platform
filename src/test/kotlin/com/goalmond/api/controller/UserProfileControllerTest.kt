package com.goalmond.api.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.goalmond.api.domain.dto.CompleteUserProfileResponse
import com.goalmond.api.domain.dto.EducationRequest
import com.goalmond.api.domain.dto.PreferenceRequest
import com.goalmond.api.domain.dto.ProfileUpdateRequest
import com.goalmond.api.service.UserProfileService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal

@WebMvcTest(UserProfileController::class)
@ActiveProfiles("local", "test")
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
class UserProfileControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var userProfileService: UserProfileService

    @MockBean
    private lateinit var jwtAuthenticationFilter: com.goalmond.api.config.JwtAuthenticationFilter

    private val testUserId = "550e8400-e29b-41d4-a716-446655440000"

    @BeforeEach
    fun setUp() {
        val auth = UsernamePasswordAuthenticationToken(testUserId, null, emptyList())
        SecurityContextHolder.getContext().authentication = auth
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `GET profile - 인증된 사용자 프로필 조회 성공`() {
        val response = CompleteUserProfileResponse(null, null, null)
        whenever(userProfileService.getUserProfile(any())).thenReturn(response)

        mockMvc.perform(get("/api/v1/user/profile"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").exists())

        verify(userProfileService).getUserProfile(any())
    }

    @Test
    fun `updateProfile - 200 OK`() {
        val request = ProfileUpdateRequest(mbti = "INTJ", tags = "체계적", bio = "소개")

        mockMvc.perform(
            put("/api/v1/user/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))

        verify(userProfileService).updateProfile(any(), any())
    }

    @Test
    fun `saveEducation - 200 OK`() {
        val request = EducationRequest(
            schoolName = "테스트고",
            schoolLocation = "서울",
            gpa = BigDecimal("3.5"),
            degree = "고등학교"
        )

        mockMvc.perform(
            post("/api/v1/user/education")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))

        verify(userProfileService).saveEducation(any(), any())
    }

    @Test
    fun `savePreference - 200 OK`() {
        val request = PreferenceRequest(
            targetProgram = "cc",
            targetMajor = "CS",
            budgetUsd = 50000
        )

        mockMvc.perform(
            post("/api/v1/user/preference")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))

        verify(userProfileService).savePreference(any(), any())
    }

    @Test
    fun `PUT profile - Validation 실패 시 400 반환`() {
        val request = ProfileUpdateRequest(mbti = "A".repeat(21), tags = null, bio = null)

        mockMvc.perform(
            put("/api/v1/user/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `POST education - 필수 필드 누락 시 400 반환`() {
        val request = mapOf<String, Any?>(
            "schoolLocation" to "서울",
            "degree" to "고등학교"
        )

        mockMvc.perform(
            post("/api/v1/user/education")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `POST preference - 예산 음수 시 400 반환`() {
        val request = PreferenceRequest(
            targetProgram = "cc",
            targetMajor = "CS",
            budgetUsd = -1
        )

        mockMvc.perform(
            post("/api/v1/user/preference")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }
}
