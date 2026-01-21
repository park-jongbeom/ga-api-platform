package com.goalmond.ai.domain.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.*

/**
 * AI 상담 요청 DTO
 */
@Schema(description = "AI 상담 요청")
data class ConsultantRequest(
    
    @field:NotBlank(message = "메시지는 필수입니다.")
    @field:Size(min = 1, max = 2000, message = "메시지는 1자 이상 2000자 이하여야 합니다.")
    @Schema(description = "사용자 메시지", example = "미국 유학에 대해 알려주세요.", required = true)
    val message: String,
    
    @Schema(description = "대화 세션 ID (없으면 새로 생성)", example = "123e4567-e89b-12d3-a456-426614174000")
    val conversationId: UUID? = null
)

/**
 * 새 대화 세션 생성 요청 DTO
 */
@Schema(description = "새 대화 세션 생성 요청")
data class CreateConversationRequest(
    
    @field:Size(max = 255, message = "제목은 255자 이하여야 합니다.")
    @Schema(description = "대화 제목", example = "미국 유학 상담")
    val title: String? = null
)
