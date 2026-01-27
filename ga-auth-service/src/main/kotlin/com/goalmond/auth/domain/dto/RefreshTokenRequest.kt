package com.goalmond.auth.domain.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "토큰 갱신 요청")
data class RefreshTokenRequest(
    @field:Schema(description = "Refresh Token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    val refreshToken: String
)
