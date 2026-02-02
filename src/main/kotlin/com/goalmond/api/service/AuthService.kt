package com.goalmond.api.service

import com.goalmond.api.config.JwtUtil
import com.goalmond.api.domain.dto.AuthResponse
import com.goalmond.api.domain.dto.LoginRequest
import com.goalmond.api.domain.dto.SignupRequest
import com.goalmond.api.domain.dto.UserSummary
import com.goalmond.api.domain.entity.User
import com.goalmond.api.repository.UserRepository
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
@Profile("local", "lightsail")
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtUtil: JwtUtil
) {
    fun signup(request: SignupRequest): AuthResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw AuthException("이미 등록된 이메일입니다", "EMAIL_ALREADY_EXISTS")
        }
        val user = User(
            email = request.email,
            fullName = request.email.substringBefore("@"),
            passwordHash = passwordEncoder.encode(request.password)
        )
        val saved = userRepository.save(user)
        val token = jwtUtil.generateToken(saved.id.toString(), saved.email)
        return AuthResponse(
            token = token,
            user = UserSummary(id = saved.id.toString(), email = saved.email, fullName = saved.fullName)
        )
    }

    fun login(request: LoginRequest): AuthResponse {
        val user = userRepository.findByEmail(request.email)
            ?: throw AuthException("이메일 또는 비밀번호가 올바르지 않습니다", "INVALID_CREDENTIALS")
        val hash = user.passwordHash
            ?: throw AuthException("이메일 또는 비밀번호가 올바르지 않습니다", "INVALID_CREDENTIALS")
        if (!passwordEncoder.matches(request.password, hash)) {
            throw AuthException("이메일 또는 비밀번호가 올바르지 않습니다", "INVALID_CREDENTIALS")
        }
        val userId = user.id ?: throw AuthException("사용자 정보 오류", "INVALID_CREDENTIALS")
        val token = jwtUtil.generateToken(userId.toString(), user.email)
        return AuthResponse(
            token = token,
            user = UserSummary(id = user.id.toString(), email = user.email, fullName = user.fullName)
        )
    }
}

class AuthException(message: String, val code: String) : RuntimeException(message)
