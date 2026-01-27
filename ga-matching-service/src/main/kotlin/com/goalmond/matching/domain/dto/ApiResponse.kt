package com.goalmond.matching.domain.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "공통 API 응답 형식")
data class ApiResponse<T>(
    @field:Schema(description = "요청 성공 여부", example = "true")
    val success: Boolean,
    @field:Schema(description = "응답 데이터")
    val data: T? = null,
    @field:Schema(description = "에러 코드", example = "INVALID_PROGRAM_TYPE")
    val code: String? = null,
    @field:Schema(description = "에러 메시지", example = "허용되지 않은 프로그램 유형입니다.")
    val message: String? = null,
    @field:Schema(description = "타임스탬프", example = "2024-01-01T00:00:00Z")
    val timestamp: Instant? = null
)
