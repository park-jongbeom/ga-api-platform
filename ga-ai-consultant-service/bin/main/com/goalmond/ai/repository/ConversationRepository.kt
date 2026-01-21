package com.goalmond.ai.repository

import com.goalmond.ai.domain.entity.Conversation
import com.goalmond.ai.domain.entity.ConversationStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

/**
 * 상담 세션 Repository
 * 
 * 테넌트 격리를 보장하는 쿼리 메서드를 제공합니다.
 */
@Repository
interface ConversationRepository : JpaRepository<Conversation, UUID> {
    
    /**
     * 사용자 ID와 테넌트 ID로 대화 세션 조회
     */
    fun findByUserIdAndTenantId(userId: String, tenantId: String): List<Conversation>
    
    /**
     * 특정 상태의 대화 세션 조회 (테넌트 격리)
     */
    fun findByUserIdAndTenantIdAndStatus(
        userId: String, 
        tenantId: String, 
        status: ConversationStatus
    ): List<Conversation>
    
    /**
     * 사용자의 활성 대화 세션 조회
     */
    @Query("SELECT c FROM Conversation c WHERE c.userId = :userId AND c.tenantId = :tenantId AND c.status = 'ACTIVE' ORDER BY c.updatedAt DESC")
    fun findActiveConversations(
        @Param("userId") userId: String,
        @Param("tenantId") tenantId: String
    ): List<Conversation>
    
    /**
     * 대화 세션 ID로 조회 (테넌트 격리)
     */
    @Query("SELECT c FROM Conversation c WHERE c.id = :id AND c.tenantId = :tenantId")
    fun findByIdAndTenantId(
        @Param("id") id: UUID,
        @Param("tenantId") tenantId: String
    ): Optional<Conversation>
}
