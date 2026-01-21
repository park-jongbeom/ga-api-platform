package com.goalmond.ai.domain.dto

import com.goalmond.ai.domain.entity.Conversation
import com.goalmond.ai.domain.entity.ConversationStatus
import com.goalmond.ai.domain.entity.Message
import com.goalmond.ai.domain.entity.MessageRole
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.*

/**
 * AI 상담 응답 DTO
 */
@Schema(description = "AI 상담 응답")
data class ConsultantResponse(
    
    @Schema(description = "AI 응답 메시지", example = "미국 유학은...")
    val response: String,
    
    @Schema(description = "대화 세션 ID")
    val conversationId: UUID,
    
    @Schema(description = "민감정보 포함 여부")
    val hasSensitiveData: Boolean,
    
    @Schema(description = "참조된 문서 수")
    val relevantDocumentsCount: Int,
    
    @Schema(description = "응답 생성 시간")
    val timestamp: LocalDateTime = LocalDateTime.now()
)

/**
 * 대화 세션 응답 DTO
 */
@Schema(description = "대화 세션 정보")
data class ConversationResponse(
    
    @Schema(description = "대화 세션 ID")
    val id: UUID,
    
    @Schema(description = "대화 제목")
    val title: String?,
    
    @Schema(description = "대화 상태")
    val status: ConversationStatus,
    
    @Schema(description = "생성 시간")
    val createdAt: LocalDateTime,
    
    @Schema(description = "수정 시간")
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(conversation: Conversation): ConversationResponse {
            return ConversationResponse(
                id = conversation.id!!,
                title = conversation.title,
                status = conversation.status,
                createdAt = conversation.createdAt,
                updatedAt = conversation.updatedAt
            )
        }
    }
}

/**
 * 메시지 응답 DTO
 */
@Schema(description = "메시지 정보")
data class MessageResponse(
    
    @Schema(description = "메시지 ID")
    val id: UUID,
    
    @Schema(description = "메시지 역할 (USER, ASSISTANT)")
    val role: MessageRole,
    
    @Schema(description = "메시지 내용")
    val content: String,
    
    @Schema(description = "생성 시간")
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(message: Message): MessageResponse {
            return MessageResponse(
                id = message.id!!,
                role = message.role,
                content = message.llmResponse ?: message.originalContent,
                createdAt = message.createdAt
            )
        }
    }
}

/**
 * 대화 내역 응답 DTO
 */
@Schema(description = "대화 내역")
data class ConversationHistoryResponse(
    
    @Schema(description = "대화 세션 정보")
    val conversation: ConversationResponse,
    
    @Schema(description = "메시지 목록")
    val messages: List<MessageResponse>
)
