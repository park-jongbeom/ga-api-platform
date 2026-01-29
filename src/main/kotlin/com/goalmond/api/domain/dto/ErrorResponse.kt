package com.goalmond.api.domain.dto

import java.time.Instant

data class ErrorResponse(
    val success: Boolean = false,
    val code: String,
    val message: String,
    val timestamp: Instant
)
