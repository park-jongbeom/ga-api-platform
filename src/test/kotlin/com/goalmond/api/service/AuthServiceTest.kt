package com.goalmond.api.service

import com.goalmond.api.config.JwtUtil
import com.goalmond.api.domain.dto.AuthResponse
import com.goalmond.api.domain.dto.LoginRequest
import com.goalmond.api.domain.dto.SignupRequest
import com.goalmond.api.domain.dto.UserSummary
import com.goalmond.api.domain.entity.User
import com.goalmond.api.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.UUID

class AuthServiceTest {

    private val userRepository = mockk<UserRepository>()
    private val passwordEncoder = mockk<PasswordEncoder>()
    private val jwtUtil = mockk<JwtUtil>()

    private val authService = AuthService(userRepository, passwordEncoder, jwtUtil)

    @Test
    fun `signup - 성공 시 토큰과 사용자 정보 반환`() {
        val request = SignupRequest(email = "test@example.com", password = "password123")
        every { userRepository.existsByEmail(request.email) } returns false
        every { passwordEncoder.encode(request.password) } returns "hashed"
        val savedUser = User(
            id = UUID.randomUUID(),
            email = request.email,
            fullName = "test",
            passwordHash = "hashed"
        )
        every { userRepository.save(any()) } returns savedUser
        every { jwtUtil.generateToken(savedUser.id.toString(), savedUser.email) } returns "jwt-token"

        val result = authService.signup(request)

        assertEquals("jwt-token", result.token)
        assertEquals(savedUser.id.toString(), result.user.id)
        assertEquals(request.email, result.user.email)
        verify(exactly = 1) { userRepository.save(any()) }
    }

    @Test
    fun `signup - 이메일 중복 시 AuthException`() {
        val request = SignupRequest(email = "dup@example.com", password = "password123")
        every { userRepository.existsByEmail(request.email) } returns true

        val ex = assertThrows(AuthException::class.java) { authService.signup(request) }

        assertEquals("EMAIL_ALREADY_EXISTS", ex.code)
        assertEquals("이미 등록된 이메일입니다", ex.message)
        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `login - 성공 시 토큰 반환`() {
        val request = LoginRequest(email = "test@example.com", password = "password123")
        val user = User(
            id = UUID.randomUUID(),
            email = request.email,
            fullName = "test",
            passwordHash = "hashed"
        )
        every { userRepository.findByEmail(request.email) } returns user
        every { passwordEncoder.matches(request.password, user.passwordHash) } returns true
        every { jwtUtil.generateToken(user.id.toString(), user.email) } returns "jwt-token"

        val result = authService.login(request)

        assertEquals("jwt-token", result.token)
        assertEquals(user.id.toString(), result.user.id)
    }

    @Test
    fun `login - 존재하지 않는 이메일 시 AuthException`() {
        val request = LoginRequest(email = "none@example.com", password = "password123")
        every { userRepository.findByEmail(request.email) } returns null

        val ex = assertThrows(AuthException::class.java) { authService.login(request) }

        assertEquals("INVALID_CREDENTIALS", ex.code)
        verify(exactly = 0) { passwordEncoder.matches(any(), any()) }
    }

    @Test
    fun `login - 비밀번호 불일치 시 AuthException`() {
        val request = LoginRequest(email = "test@example.com", password = "wrong")
        val user = User(id = UUID.randomUUID(), email = request.email, fullName = "test", passwordHash = "hashed")
        every { userRepository.findByEmail(request.email) } returns user
        every { passwordEncoder.matches(request.password, user.passwordHash) } returns false

        val ex = assertThrows(AuthException::class.java) { authService.login(request) }

        assertEquals("INVALID_CREDENTIALS", ex.code)
    }
}
