package com.goalmond.ai.domain.entity

import com.goalmond.common.entity.BaseEntity
import jakarta.persistence.*

/**
 * 상담 세션 Entity
 * 
 * 사용자와 AI 간의 대화 세션을 나타냅니다.
 * 멀티테넌트 환경을 위해 tenant_id를 포함합니다.
 */
@Entity
@Table(
    name = "conversations",
    indexes = [
        Index(name = "idx_user_tenant", columnList = "user_id,tenant_id"),
        Index(name = "idx_created_at", columnList = "created_at"),
        Index(name = "idx_tenant", columnList = "tenant_id")
    ]
)
class Conversation(
    
    @Column(name = "user_id", columnDefinition = "BINARY(16)", nullable = false)
    var userId: String,
    
    @Column(name = "tenant_id", nullable = false, length = 50)
    var tenantId: String,
    
    @Column(nullable = true, length = 255)
    var title: String? = null,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ConversationStatus = ConversationStatus.ACTIVE,
    
    @OneToMany(mappedBy = "conversation", cascade = [CascadeType.ALL], orphanRemoval = true)
    var messages: MutableList<Message> = mutableListOf()
    
) : BaseEntity() {
    
    /**
     * 메시지 추가 헬퍼 메서드
     */
    fun addMessage(message: Message) {
        messages.add(message)
        message.conversation = this
    }
    
    /**
     * 대화 종료
     */
    fun close() {
        this.status = ConversationStatus.CLOSED
    }
}

/**
 * 상담 세션 상태
 */
enum class ConversationStatus {
    /** 활성 상태 */
    ACTIVE,
    
    /** 종료 상태 */
    CLOSED
}
