package com.goalmond.api.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.goalmond.api.domain.dto.AuthResponse
import com.goalmond.api.domain.dto.LoginRequest
import com.goalmond.api.domain.dto.SignupRequest
import com.goalmond.api.domain.dto.UserSummary
import com.goalmond.api.service.AuthException
import com.goalmond.api.service.AuthService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

@WebMvcTest(AuthController::class)
@ActiveProfiles("local", "test")
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var authService: AuthService

    @MockBean
    private lateinit var jwtAuthenticationFilter: com.goalmond.api.config.JwtAuthenticationFilter

    @Test
    fun `signup - 성공 시 200과 토큰 반환`() {
        val request = SignupRequest(email = "test@example.com", password = "password123")
        val response = AuthResponse(
            token = "jwt-token",
            user = UserSummary(id = "uuid", email = request.email, fullName = "test")
        )
        whenever(authService.signup(any())).thenReturn(response)

        mockMvc.perform(
            post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.token").value("jwt-token"))
            .andExpect(jsonPath("$.data.user.email").value(request.email))
    }

    @Test
    fun `signup - 이메일 중복 시 400`() {
        val request = SignupRequest(email = "dup@example.com", password = "password123")
        whenever(authService.signup(any())).thenThrow(AuthException("이미 등록된 이메일입니다", "EMAIL_ALREADY_EXISTS"))

        mockMvc.perform(
            post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"))
    }

    @Test
    fun `signup - validation 실패 시 400`() {
        val invalidRequest = """{"email":"invalid-email","password":"short"}"""

        mockMvc.perform(
            post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `login - 성공 시 200과 토큰 반환`() {
        val request = LoginRequest(email = "test@example.com", password = "password123")
        val response = AuthResponse(
            token = "jwt-token",
            user = UserSummary(id = "uuid", email = request.email, fullName = "test")
        )
        whenever(authService.login(any())).thenReturn(response)

        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.token").value("jwt-token"))
    }

    @Test
    fun `login - 인증 실패 시 401`() {
        val request = LoginRequest(email = "test@example.com", password = "wrong")
        whenever(authService.login(any())).thenThrow(AuthException("이메일 또는 비밀번호가 올바르지 않습니다", "INVALID_CREDENTIALS"))

        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
    }
}
