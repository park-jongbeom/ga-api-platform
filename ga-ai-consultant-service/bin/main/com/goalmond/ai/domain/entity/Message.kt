package com.goalmond.ai.domain.entity

import com.goalmond.common.entity.BaseEntity
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

/**
 * 메시지 Entity
 * 
 * 사용자와 AI 간의 개별 메시지를 나타냅니다.
 * 마스킹 전 원본 콘텐츠와 마스킹 후 콘텐츠를 모두 저장하여 추적 가능성을 보장합니다.
 */
@Entity
@Table(
    name = "messages",
    indexes = [
        Index(name = "idx_conversation", columnList = "conversation_id"),
        Index(name = "idx_created_at", columnList = "created_at")
    ]
)
class Message(
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    var conversation: Conversation,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var role: MessageRole,
    
    @Column(name = "original_content", columnDefinition = "TEXT", nullable = false)
    var originalContent: String,
    
    @Column(name = "masked_content", columnDefinition = "TEXT")
    var maskedContent: String? = null,
    
    @Column(name = "llm_response", columnDefinition = "TEXT")
    var llmResponse: String? = null,
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "masked_tokens", columnDefinition = "jsonb")
    var maskedTokens: Map<String, String>? = null
    
) : BaseEntity()

/**
 * 메시지 역할
 */
enum class MessageRole {
    /** 사용자 메시지 */
    USER,
    
    /** AI 어시스턴트 메시지 */
    ASSISTANT,
    
    /** 시스템 메시지 */
    SYSTEM
}
