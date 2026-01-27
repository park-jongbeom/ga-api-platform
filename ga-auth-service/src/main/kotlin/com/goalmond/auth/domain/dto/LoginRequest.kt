package com.goalmond.auth.domain.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "로그인 요청")
data class LoginRequest(
    @field:Schema(description = "사용자 이메일", example = "user@example.com")
    val email: String,
    @field:Schema(description = "비밀번호", example = "securePassword123!")
    val password: String
)
