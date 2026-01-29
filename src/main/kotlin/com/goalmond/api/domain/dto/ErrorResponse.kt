package com.goalmond.api.domain.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "표준 에러 응답")
data class ErrorResponse(
    @field:Schema(description = "요청 성공 여부", example = "false")
    val success: Boolean = false,
    @field:Schema(description = "에러 코드", example = "INVALID_PROGRAM_TYPE")
    val code: String,
    @field:Schema(description = "에러 메시지", example = "허용되지 않은 프로그램 유형입니다.")
    val message: String,
    @field:Schema(description = "타임스탬프", example = "2024-01-01T00:00:00Z")
    val timestamp: Instant
)
