package com.goalmond.ai.repository

import com.goalmond.ai.domain.entity.Conversation
import com.goalmond.ai.domain.entity.Message
import com.goalmond.ai.domain.entity.MessageRole
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles

/**
 * MessageRepository 테스트
 * 
 * 마스킹 데이터 저장 및 조회 기능을 검증합니다.
 */
@DataJpaTest
@ActiveProfiles("test")
class MessageRepositoryTest {

    @Autowired
    private lateinit var messageRepository: MessageRepository
    
    @Autowired
    private lateinit var conversationRepository: ConversationRepository

    @Test
    fun `메시지 생성 및 조회 테스트`() {
        // Given
        val conversation = conversationRepository.save(
            Conversation(userId = "user-123", tenantId = "tenant-a")
        )
        
        val message = Message(
            conversation = conversation,
            role = MessageRole.USER,
            originalContent = "My passport number is M12345678",
            maskedContent = "My passport number is [PASSPORT_001]",
            maskedTokens = mapOf("[PASSPORT_001]" to "M12345678")
        )
        
        // When
        val saved = messageRepository.save(message)
        
        // Then
        assertNotNull(saved.id)
        assertEquals(MessageRole.USER, saved.role)
        assertEquals("My passport number is M12345678", saved.originalContent)
        assertEquals("My passport number is [PASSPORT_001]", saved.maskedContent)
        assertEquals(1, saved.maskedTokens?.size)
    }

    @Test
    fun `대화 세션별 메시지 조회 테스트`() {
        // Given
        val conversation = conversationRepository.save(
            Conversation(userId = "user-123", tenantId = "tenant-a")
        )
        
        messageRepository.save(
            Message(conversation = conversation, role = MessageRole.USER, originalContent = "Hello")
        )
        messageRepository.save(
            Message(conversation = conversation, role = MessageRole.ASSISTANT, originalContent = "Hi there!")
        )
        
        // When
        val messages = messageRepository.findByConversationId(conversation.id!!)
        
        // Then
        assertEquals(2, messages.size)
        assertEquals(MessageRole.USER, messages[0].role)
        assertEquals(MessageRole.ASSISTANT, messages[1].role)
    }

    @Test
    fun `마스킹 토큰 JSON 저장 테스트`() {
        // Given
        val conversation = conversationRepository.save(
            Conversation(userId = "user-123", tenantId = "tenant-a")
        )
        
        val maskedTokens = mapOf(
            "[PASSPORT_001]" to "M12345678",
            "[EMAIL_001]" to "test@example.com",
            "[PHONE_001]" to "010-1234-5678"
        )
        
        val message = Message(
            conversation = conversation,
            role = MessageRole.USER,
            originalContent = "Original text with sensitive data",
            maskedContent = "Masked text with tokens",
            maskedTokens = maskedTokens
        )
        
        // When
        val saved = messageRepository.save(message)
        val retrieved = messageRepository.findById(saved.id!!).get()
        
        // Then
        assertEquals(3, retrieved.maskedTokens?.size)
        assertEquals("M12345678", retrieved.maskedTokens?.get("[PASSPORT_001]"))
        assertEquals("test@example.com", retrieved.maskedTokens?.get("[EMAIL_001]"))
        assertEquals("010-1234-5678", retrieved.maskedTokens?.get("[PHONE_001]"))
    }

    @Test
    fun `LLM 응답 저장 테스트`() {
        // Given
        val conversation = conversationRepository.save(
            Conversation(userId = "user-123", tenantId = "tenant-a")
        )
        
        val message = Message(
            conversation = conversation,
            role = MessageRole.USER,
            originalContent = "Tell me about studying abroad"
        )
        
        // When
        val saved = messageRepository.save(message)
        saved.llmResponse = "Studying abroad is a great opportunity..."
        val updated = messageRepository.save(saved)
        
        // Then
        assertNotNull(updated.llmResponse)
        assertEquals("Studying abroad is a great opportunity...", updated.llmResponse)
    }
}
