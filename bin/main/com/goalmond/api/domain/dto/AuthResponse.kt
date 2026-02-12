package com.goalmond.api.domain.dto

data class AuthResponse(
    val token: String,
    val user: UserSummary
)

data class UserSummary(
    val id: String,
    val email: String,
    val fullName: String
)
