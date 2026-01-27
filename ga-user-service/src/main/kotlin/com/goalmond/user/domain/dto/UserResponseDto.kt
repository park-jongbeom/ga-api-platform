package com.goalmond.user.domain.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "사용자 기본 정보 응답")
data class UserResponseDto(
    @field:Schema(description = "사용자 UUID", example = "123e4567-e89b-12d3-a456-426614174000")
    val userId: String,
    @field:Schema(description = "이메일", example = "user@example.com")
    val email: String,
    @field:Schema(description = "이름", example = "홍길동")
    val name: String,
    @field:Schema(description = "생성일시", example = "2024-01-01T00:00:00")
    val createdAt: LocalDateTime
)
