package com.goalmond.common.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/**
 * 공통 API 응답 DTO
 */
@Schema(description = "공통 API 응답 형식")
data class ApiResponse<T>(
    @Schema(description = "요청 성공 여부", example = "true")
    val success: Boolean,
    
    @Schema(description = "응답 데이터", nullable = true)
    val data: T? = null,
    
    @Schema(description = "응답 메시지", nullable = true, example = "요청이 성공적으로 처리되었습니다.")
    val message: String? = null,
    
    @Schema(description = "응답 시간", example = "2024-01-01T00:00:00")
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
