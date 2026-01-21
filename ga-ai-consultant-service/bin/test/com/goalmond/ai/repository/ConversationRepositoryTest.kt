package com.goalmond.ai.repository

import com.goalmond.ai.domain.entity.Conversation
import com.goalmond.ai.domain.entity.ConversationStatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles

/**
 * ConversationRepository 테스트
 * 
 * 테넌트 격리 및 CRUD 기능을 검증합니다.
 */
@DataJpaTest
@ActiveProfiles("test")
class ConversationRepositoryTest {

    @Autowired
    private lateinit var conversationRepository: ConversationRepository

    @Test
    fun `대화 세션 생성 및 조회 테스트`() {
        // Given
        val conversation = Conversation(
            userId = "user-123",
            tenantId = "tenant-a",
            title = "유학 상담",
            status = ConversationStatus.ACTIVE
        )
        
        // When
        val saved = conversationRepository.save(conversation)
        
        // Then
        assertNotNull(saved.id)
        assertEquals("user-123", saved.userId)
        assertEquals("tenant-a", saved.tenantId)
        assertEquals(ConversationStatus.ACTIVE, saved.status)
    }

    @Test
    fun `테넌트 격리 테스트 - 다른 테넌트 데이터 조회 불가`() {
        // Given
        conversationRepository.save(
            Conversation(userId = "user-123", tenantId = "tenant-a", title = "Tenant A 대화")
        )
        conversationRepository.save(
            Conversation(userId = "user-123", tenantId = "tenant-b", title = "Tenant B 대화")
        )
        
        // When
        val tenantAConversations = conversationRepository.findByUserIdAndTenantId("user-123", "tenant-a")
        val tenantBConversations = conversationRepository.findByUserIdAndTenantId("user-123", "tenant-b")
        
        // Then
        assertEquals(1, tenantAConversations.size)
        assertEquals("Tenant A 대화", tenantAConversations[0].title)
        
        assertEquals(1, tenantBConversations.size)
        assertEquals("Tenant B 대화", tenantBConversations[0].title)
    }

    @Test
    fun `활성 대화 세션 조회 테스트`() {
        // Given
        conversationRepository.save(
            Conversation(userId = "user-123", tenantId = "tenant-a", status = ConversationStatus.ACTIVE)
        )
        conversationRepository.save(
            Conversation(userId = "user-123", tenantId = "tenant-a", status = ConversationStatus.CLOSED)
        )
        
        // When
        val activeConversations = conversationRepository.findActiveConversations("user-123", "tenant-a")
        
        // Then
        assertEquals(1, activeConversations.size)
        assertEquals(ConversationStatus.ACTIVE, activeConversations[0].status)
    }

    @Test
    fun `대화 세션 상태 변경 테스트`() {
        // Given
        val conversation = conversationRepository.save(
            Conversation(userId = "user-123", tenantId = "tenant-a", status = ConversationStatus.ACTIVE)
        )
        
        // When
        conversation.close()
        val updated = conversationRepository.save(conversation)
        
        // Then
        assertEquals(ConversationStatus.CLOSED, updated.status)
    }
}
