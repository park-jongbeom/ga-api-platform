package com.goalmond.audit.domain.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "감사 로그 응답")
data class AuditLogResponse(
    @field:Schema(description = "감사 로그 목록")
    val logs: List<AuditLogItem>,
    @field:Schema(description = "페이지네이션 정보")
    val pagination: Pagination
) {
    @Schema(description = "감사 로그 항목")
    data class AuditLogItem(
        @field:Schema(description = "로그 ID", example = "audit-1")
        val id: String,
        @field:Schema(description = "테이블 이름", example = "users")
        val tableName: String,
        @field:Schema(description = "레코드 ID", example = "123e4567-e89b-12d3-a456-426614174000")
        val recordId: String,
        @field:Schema(description = "작업 유형", example = "INSERT")
        val action: String,
        @field:Schema(description = "이전 값")
        val oldValues: Map<String, String>?,
        @field:Schema(description = "변경 값")
        val newValues: Map<String, String>?,
        @field:Schema(description = "작업자 ID", example = "admin-1")
        val userId: String,
        @field:Schema(description = "작업 시각", example = "2024-01-01T00:00:00")
        val timestamp: LocalDateTime
    )

    @Schema(description = "페이지네이션 정보")
    data class Pagination(
        @field:Schema(description = "전체 항목 수", example = "100")
        val totalElements: Int,
        @field:Schema(description = "전체 페이지 수", example = "5")
        val totalPages: Int,
        @field:Schema(description = "현재 페이지 번호", example = "0")
        val currentPage: Int,
        @field:Schema(description = "페이지 크기", example = "20")
        val pageSize: Int
    )
}
