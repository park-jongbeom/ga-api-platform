package com.goalmond.ai.repository

import com.goalmond.ai.domain.entity.Message
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

/**
 * 메시지 Repository
 */
@Repository
interface MessageRepository : JpaRepository<Message, UUID> {
    
    /**
     * 특정 대화 세션의 모든 메시지 조회
     */
    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId ORDER BY m.createdAt ASC")
    fun findByConversationId(@Param("conversationId") conversationId: UUID): List<Message>
    
    /**
     * 특정 대화 세션의 최근 N개 메시지 조회
     */
    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId ORDER BY m.createdAt DESC LIMIT :limit")
    fun findRecentMessagesByConversationId(
        @Param("conversationId") conversationId: UUID,
        @Param("limit") limit: Int
    ): List<Message>
}
