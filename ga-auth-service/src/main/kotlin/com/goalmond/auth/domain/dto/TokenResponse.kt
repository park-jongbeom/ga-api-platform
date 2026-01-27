package com.goalmond.auth.domain.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "토큰 응답")
data class TokenResponse(
    @field:Schema(
        description = "Access Token",
        example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
    )
    val accessToken: String,
    @field:Schema(
        description = "Refresh Token",
        example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
    )
    val refreshToken: String,
    @field:Schema(description = "토큰 타입", example = "Bearer")
    val tokenType: String,
    @field:Schema(description = "Access Token 유효 시간(초)", example = "3600")
    val expiresIn: Int
)
