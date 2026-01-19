package com.goalmond.common.dto

import java.time.LocalDateTime

/**
 * 공통 API 응답 DTO
 */
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val timestamp: LocalDateTime = LocalDateTime.now()
) {
    companion object {
        fun <T> success(data: T, message: String? = null): ApiResponse<T> {
            return ApiResponse(true, data, message)
        }

        fun <T> error(message: String): ApiResponse<T> {
            return ApiResponse(false, null, message)
        }
    }
}
