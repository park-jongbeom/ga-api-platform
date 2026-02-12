package com.goalmond.api.domain.dto

import java.time.Instant

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val code: String? = null,
    val message: String? = null,
    val timestamp: Instant? = null
)
