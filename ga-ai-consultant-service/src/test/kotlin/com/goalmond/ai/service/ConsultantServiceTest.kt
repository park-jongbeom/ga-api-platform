package com.goalmond.ai.service

import com.goalmond.ai.domain.dto.MaskedData
import com.goalmond.ai.domain.entity.Conversation
import com.goalmond.ai.domain.entity.ConversationStatus
import com.goalmond.ai.domain.entity.Document
import com.goalmond.ai.domain.entity.Message
import com.goalmond.ai.repository.ConversationRepository
import com.goalmond.ai.repository.MessageRepository
import com.goalmond.ai.security.validator.InputSanitizer
import com.goalmond.ai.security.validator.ValidationResult
import dev.langchain4j.model.chat.ChatLanguageModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import java.util.*

/**
 * ConsultantService 테스트
 * 
 * AI 상담 통합 플로우를 검증합니다.
 */
class ConsultantServiceTest {

    private lateinit var consultantService: ConsultantService
    private lateinit var conversationRepository: ConversationRepository
    private lateinit var messageRepository: MessageRepository
    private lateinit var maskingService: MaskingService
    private lateinit var ragService: RagService
    private lateinit var inputSanitizer: InputSanitizer
    private lateinit var chatLanguageModel: ChatLanguageModel
    private lateinit var promptTemplateManager: com.goalmond.ai.domain.prompt.PromptTemplateManager

    @BeforeEach
    fun setUp() {
        conversationRepository = mockk()
        messageRepository = mockk()
        maskingService = mockk()
        ragService = mockk()
        inputSanitizer = mockk()
        chatLanguageModel = mockk()
        promptTemplateManager = mockk()
        
        // Mock promptTemplateManager to return a sample prompt
        every { promptTemplateManager.createStudyAbroadPrompt(any(), any()) } returns "Generated prompt with context"
        
        consultantService = ConsultantService(
            conversationRepository,
            messageRepository,
            maskingService,
            ragService,
            inputSanitizer,
            chatLanguageModel,
            promptTemplateManager
        )
    }

    @Test
    fun `AI 상담 처리 성공 테스트`() {
        // Given
        val conversationId = UUID.randomUUID()
        val userId = "user-123"
        val tenantId = "tenant-a"
        val userMessage = "미국 유학에 대해 알려주세요."
        
        val conversation = Conversation(userId = userId, tenantId = tenantId)
        val maskedData = MaskedData(userMessage, userMessage, emptyMap())
        val documents = listOf(
            Document(title = "미국 유학 가이드", content = "미국 유학 정보...")
        )
        val llmResponse = "미국 유학은..."
        
        every { inputSanitizer.validate(userMessage) } returns ValidationResult(isValid = true)
        every { 
            conversationRepository.findByIdAndTenantId(conversationId, tenantId) 
        } returns Optional.of(conversation)
        every { maskingService.maskSensitiveData(userMessage) } returns maskedData
        every { messageRepository.save(any()) } returns mockk()
        every { ragService.searchSimilarDocuments(any(), eq(tenantId), eq(5)) } returns documents
        every { ragService.formatContextForLlm(documents) } returns "RAG Context"
        every { chatLanguageModel.generate(any<String>()) } returns llmResponse
        
        // When
        val response = consultantService.processChat(conversationId, userMessage, userId, tenantId)
        
        // Then
        assertEquals(llmResponse, response.response)
        assertEquals(conversationId, response.conversationId)
        assertEquals(1, response.relevantDocumentsCount)
        
        verify { inputSanitizer.validate(userMessage) }
        verify { maskingService.maskSensitiveData(userMessage) }
        verify(exactly = 2) { messageRepository.save(any()) } // User + Assistant messages
        verify { chatLanguageModel.generate(any<String>()) }
    }

    @Test
    fun `민감정보 포함 상담 처리 테스트`() {
        // Given
        val conversationId = UUID.randomUUID()
        val userId = "user-123"
        val tenantId = "tenant-a"
        val userMessage = "My passport is M12345678"
        
        val conversation = Conversation(userId = userId, tenantId = tenantId)
        val maskedData = MaskedData(
            original = userMessage,
            masked = "My passport is [PASSPORT_001]",
            tokens = mapOf("[PASSPORT_001]" to "M12345678")
        )
        val llmResponse = "I understand you have passport [PASSPORT_001]"
        
        every { inputSanitizer.validate(userMessage) } returns ValidationResult(isValid = true)
        every { 
            conversationRepository.findByIdAndTenantId(conversationId, tenantId) 
        } returns Optional.of(conversation)
        every { maskingService.maskSensitiveData(userMessage) } returns maskedData
        every { messageRepository.save(any()) } returns mockk()
        every { ragService.searchSimilarDocuments(any(), any(), any()) } returns emptyList()
        every { ragService.formatContextForLlm(any()) } returns "No documents"
        every { chatLanguageModel.generate(any<String>()) } returns llmResponse
        
        // When
        val response = consultantService.processChat(conversationId, userMessage, userId, tenantId)
        
        // Then
        assertTrue(response.hasSensitiveData)
        verify { maskingService.maskSensitiveData(userMessage) }
    }

    @Test
    fun `입력값 검증 실패 테스트`() {
        // Given
        val conversationId = UUID.randomUUID()
        val userId = "user-123"
        val tenantId = "tenant-a"
        val maliciousInput = "<script>alert('XSS')</script>"
        
        every { 
            inputSanitizer.validate(maliciousInput) 
        } returns ValidationResult(isValid = false, reason = "XSS detected")
        
        // When & Then
        assertThrows(SecurityException::class.java) {
            consultantService.processChat(conversationId, maliciousInput, userId, tenantId)
        }
        
        verify { inputSanitizer.validate(maliciousInput) }
        verify(exactly = 0) { chatLanguageModel.generate(any<String>()) }
    }

    @Test
    fun `LLM 호출 실패 시 Fallback 테스트`() {
        // Given
        val conversationId = UUID.randomUUID()
        val userId = "user-123"
        val tenantId = "tenant-a"
        val userMessage = "테스트 메시지"
        
        val conversation = Conversation(userId = userId, tenantId = tenantId)
        val maskedData = MaskedData(userMessage, userMessage, emptyMap())
        
        every { inputSanitizer.validate(userMessage) } returns ValidationResult(isValid = true)
        every { 
            conversationRepository.findByIdAndTenantId(conversationId, tenantId) 
        } returns Optional.of(conversation)
        every { maskingService.maskSensitiveData(userMessage) } returns maskedData
        every { messageRepository.save(any()) } returns mockk()
        every { ragService.searchSimilarDocuments(any(), any(), any()) } returns emptyList()
        every { ragService.formatContextForLlm(any()) } returns "No documents"
        every { chatLanguageModel.generate(any<String>()) } throws RuntimeException("LLM API Error")
        
        // When
        val response = consultantService.processChat(conversationId, userMessage, userId, tenantId)
        
        // Then
        assertTrue(response.response.contains("일시적인 오류"))
        verify { chatLanguageModel.generate(any<String>()) }
    }

    @Test
    fun `새 대화 세션 생성 테스트`() {
        // Given
        val userId = "user-123"
        val tenantId = "tenant-a"
        val title = "유학 상담"
        
        val conversation = Conversation(
            userId = userId,
            tenantId = tenantId,
            title = title,
            status = ConversationStatus.ACTIVE
        )
        
        every { conversationRepository.save(any()) } returns conversation
        
        // When
        val result = consultantService.createConversation(userId, tenantId, title)
        
        // Then
        assertEquals(userId, result.userId)
        assertEquals(tenantId, result.tenantId)
        assertEquals(title, result.title)
        verify { conversationRepository.save(any()) }
    }

    @Test
    fun `대화 내역 조회 테스트`() {
        // Given
        val conversationId = UUID.randomUUID()
        val tenantId = "tenant-a"
        val conversation = Conversation(userId = "user-123", tenantId = tenantId)
        val messages = listOf(
            mockk<Message>(),
            mockk<Message>()
        )
        
        every { 
            conversationRepository.findByIdAndTenantId(conversationId, tenantId) 
        } returns Optional.of(conversation)
        every { messageRepository.findByConversationId(conversationId) } returns messages
        
        // When
        val result = consultantService.getConversationHistory(conversationId, tenantId)
        
        // Then
        assertEquals(2, result.size)
        verify { messageRepository.findByConversationId(conversationId) }
    }

    @Test
    fun `테넌트 격리 검증 - 다른 테넌트 대화 접근 불가`() {
        // Given
        val conversationId = UUID.randomUUID()
        val wrongTenantId = "wrong-tenant"
        
        every { 
            conversationRepository.findByIdAndTenantId(conversationId, wrongTenantId) 
        } returns Optional.empty()
        
        // When & Then
        assertThrows(IllegalArgumentException::class.java) {
            consultantService.getConversationHistory(conversationId, wrongTenantId)
        }
    }
}
