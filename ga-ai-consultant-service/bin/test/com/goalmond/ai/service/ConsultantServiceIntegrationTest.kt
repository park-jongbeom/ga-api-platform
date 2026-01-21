package com.goalmond.ai.service

import com.goalmond.ai.domain.entity.Conversation
import com.goalmond.ai.domain.entity.Message
import com.goalmond.ai.domain.entity.MessageRole
import com.goalmond.ai.domain.prompt.PromptTemplateManager
import com.goalmond.ai.repository.ConversationRepository
import com.goalmond.ai.repository.MessageRepository
import com.goalmond.ai.security.validator.InputSanitizer
import com.goalmond.ai.security.validator.ValidationResult
import dev.langchain4j.model.chat.ChatLanguageModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*

/**
 * ConsultantService 통합 테스트
 * 
 * 테스트 범위:
 * - PromptTemplateManager 통합
 * - 전체 AI 상담 파이프라인
 * - 보안 검증 통합
 * 
 * @author AI Consultant Team
 * @since 2026-01-21
 */
class ConsultantServiceIntegrationTest {
    
    private lateinit var conversationRepository: ConversationRepository
    private lateinit var messageRepository: MessageRepository
    private lateinit var maskingService: MaskingService
    private lateinit var ragService: RagService
    private lateinit var inputSanitizer: InputSanitizer
    private lateinit var chatLanguageModel: ChatLanguageModel
    private lateinit var promptTemplateManager: PromptTemplateManager
    private lateinit var consultantService: ConsultantService
    
    @BeforeEach
    fun setup() {
        conversationRepository = mockk()
        messageRepository = mockk()
        maskingService = mockk()
        ragService = mockk()
        inputSanitizer = mockk()
        chatLanguageModel = mockk()
        promptTemplateManager = mockk()
        
        consultantService = ConsultantService(
            conversationRepository = conversationRepository,
            messageRepository = messageRepository,
            maskingService = maskingService,
            ragService = ragService,
            inputSanitizer = inputSanitizer,
            chatLanguageModel = chatLanguageModel,
            promptTemplateManager = promptTemplateManager
        )
    }
    
    @Test
    fun `AI 상담 처리 - 정상 플로우`() {
        // Given
        val conversationId = UUID.randomUUID()
        val userMessage = "미국 대학 입학 조건은?"
        val userId = "user-001"
        val tenantId = "tenant-001"
        
        val conversation = Conversation(userId = userId, tenantId = tenantId, title = "유학 상담")
        val maskedData = mockMaskedData(original = userMessage, masked = userMessage)
        val ragContext = "미국 대학은 TOEFL 80점 이상 필요"
        val llmResponse = "미국 대학 입학을 위해서는 TOEFL 80점 이상이 필요합니다."
        val prompt = "전체 프롬프트 내용"
        
        // Mock 설정
        every { inputSanitizer.validate(userMessage) } returns ValidationResult(true, null)
        every { conversationRepository.findByIdAndTenantId(conversationId, tenantId) } returns Optional.of(conversation)
        every { maskingService.maskSensitiveData(userMessage) } returns maskedData
        every { messageRepository.save(any()) } returns mockk()
        every { ragService.searchSimilarDocuments(any(), any(), any()) } returns emptyList()
        every { ragService.formatContextForLlm(any()) } returns ragContext
        every { promptTemplateManager.createStudyAbroadPrompt(ragContext, userMessage) } returns prompt
        every { promptTemplateManager.getVersion() } returns "v1.0.0"
        every { chatLanguageModel.generate(prompt) } returns llmResponse
        
        // When
        val response = consultantService.processChat(
            conversationId = conversationId,
            userMessage = userMessage,
            userId = userId,
            tenantId = tenantId
        )
        
        // Then
        assertNotNull(response)
        assertEquals(llmResponse, response.response)
        assertEquals(conversationId, response.conversationId)
        assertEquals(0, response.relevantDocumentsCount)
        
        // 검증: PromptTemplateManager가 호출되었는지
        verify(exactly = 1) { 
            promptTemplateManager.createStudyAbroadPrompt(ragContext, userMessage) 
        }
        
        // 검증: 메시지가 저장되었는지 (사용자 + AI 응답)
        verify(exactly = 2) { messageRepository.save(any()) }
    }
    
    @Test
    fun `AI 상담 처리 - 프롬프트 템플릿 버전 로깅`() {
        // Given
        val conversationId = UUID.randomUUID()
        val userMessage = "질문"
        val userId = "user-001"
        val tenantId = "tenant-001"
        
        val conversation = Conversation(userId = userId, tenantId = tenantId, title = "상담")
        val maskedData = mockMaskedData(original = userMessage, masked = userMessage)
        
        every { inputSanitizer.validate(any()) } returns ValidationResult(true, null)
        every { conversationRepository.findByIdAndTenantId(any(), any()) } returns Optional.of(conversation)
        every { maskingService.maskSensitiveData(any()) } returns maskedData
        every { messageRepository.save(any()) } returns mockk()
        every { ragService.searchSimilarDocuments(any(), any(), any()) } returns emptyList()
        every { ragService.formatContextForLlm(any()) } returns "컨텍스트"
        every { promptTemplateManager.createStudyAbroadPrompt(any(), any()) } returns "프롬프트"
        every { promptTemplateManager.getVersion() } returns "v2.0.0"
        every { chatLanguageModel.generate(any<String>()) } returns "응답"
        
        // When
        consultantService.processChat(conversationId, userMessage, userId, tenantId)
        
        // Then
        verify(exactly = 1) { promptTemplateManager.getVersion() }
    }
    
    @Test
    fun `AI 상담 처리 - 입력 검증 실패`() {
        // Given
        val conversationId = UUID.randomUUID()
        val maliciousMessage = "<script>alert('XSS')</script>"
        val userId = "user-001"
        val tenantId = "tenant-001"
        
        every { inputSanitizer.validate(maliciousMessage) } returns ValidationResult(false, "XSS 패턴 감지")
        
        // When & Then
        val exception = assertThrows<SecurityException> {
            consultantService.processChat(conversationId, maliciousMessage, userId, tenantId)
        }
        
        assertTrue(exception.message!!.contains("입력값 검증"))
    }
    
    @Test
    fun `AI 상담 처리 - 대화 세션 없음`() {
        // Given
        val conversationId = UUID.randomUUID()
        val userMessage = "질문"
        val userId = "user-001"
        val tenantId = "tenant-001"
        
        every { inputSanitizer.validate(any()) } returns ValidationResult(true, null)
        every { conversationRepository.findByIdAndTenantId(conversationId, tenantId) } returns Optional.empty()
        
        // When & Then
        assertThrows<IllegalArgumentException> {
            consultantService.processChat(conversationId, userMessage, userId, tenantId)
        }
    }
    
    @Test
    fun `AI 상담 처리 - LLM 오류 시 안전한 응답`() {
        // Given
        val conversationId = UUID.randomUUID()
        val userMessage = "질문"
        val userId = "user-001"
        val tenantId = "tenant-001"
        
        val conversation = Conversation(userId = userId, tenantId = tenantId, title = "상담")
        val maskedData = mockMaskedData(original = userMessage, masked = userMessage)
        
        every { inputSanitizer.validate(any()) } returns ValidationResult(true, null)
        every { conversationRepository.findByIdAndTenantId(any(), any()) } returns Optional.of(conversation)
        every { maskingService.maskSensitiveData(any()) } returns maskedData
        every { messageRepository.save(any()) } returns mockk()
        every { ragService.searchSimilarDocuments(any(), any(), any()) } returns emptyList()
        every { ragService.formatContextForLlm(any()) } returns "컨텍스트"
        every { promptTemplateManager.createStudyAbroadPrompt(any(), any()) } returns "프롬프트"
        every { promptTemplateManager.getVersion() } returns "v1.0.0"
        every { chatLanguageModel.generate(any<String>()) } throws RuntimeException("OpenAI API 오류")
        
        // When
        val response = consultantService.processChat(conversationId, userMessage, userId, tenantId)
        
        // Then
        assertTrue(response.response.contains("일시적인 오류"))
        assertTrue(response.response.contains("잠시 후 다시 시도"))
    }
    
    @Test
    fun `대화 세션 생성`() {
        // Given
        val userId = "user-001"
        val tenantId = "tenant-001"
        val title = "새 유학 상담"
        
        val savedConversation = Conversation(userId = userId, tenantId = tenantId, title = title)
        every { conversationRepository.save(any()) } returns savedConversation
        
        // When
        val conversation = consultantService.createConversation(userId, tenantId, title)
        
        // Then
        assertNotNull(conversation)
        assertEquals(userId, conversation.userId)
        assertEquals(tenantId, conversation.tenantId)
        assertEquals(title, conversation.title)
    }
    
    @Test
    fun `대화 세션 생성 - 기본 제목`() {
        // Given
        val userId = "user-001"
        val tenantId = "tenant-001"
        
        val conversationSlot = slot<Conversation>()
        every { conversationRepository.save(capture(conversationSlot)) } answers { conversationSlot.captured }
        
        // When
        val conversation = consultantService.createConversation(userId, tenantId)
        
        // Then
        assertEquals("새 상담", conversation.title)
    }
    
    @Test
    fun `대화 내역 조회 - 테넌트 격리`() {
        // Given
        val conversationId = UUID.randomUUID()
        val tenantId = "tenant-001"
        
        val conversation = Conversation(userId = "user-001", tenantId = tenantId, title = "상담")
        val messages = listOf(
            Message(conversation = conversation, role = MessageRole.USER, originalContent = "질문1"),
            Message(conversation = conversation, role = MessageRole.ASSISTANT, originalContent = "답변1")
        )
        
        every { conversationRepository.findByIdAndTenantId(conversationId, tenantId) } returns Optional.of(conversation)
        every { messageRepository.findByConversationId(conversationId) } returns messages
        
        // When
        val history = consultantService.getConversationHistory(conversationId, tenantId)
        
        // Then
        assertEquals(2, history.size)
        assertEquals(MessageRole.USER, history[0].role)
        assertEquals(MessageRole.ASSISTANT, history[1].role)
    }
    
    @Test
    fun `대화 내역 조회 - 잘못된 테넌트`() {
        // Given
        val conversationId = UUID.randomUUID()
        val wrongTenantId = "wrong-tenant"
        
        every { conversationRepository.findByIdAndTenantId(conversationId, wrongTenantId) } returns Optional.empty()
        
        // When & Then
        assertThrows<IllegalArgumentException> {
            consultantService.getConversationHistory(conversationId, wrongTenantId)
        }
    }
    
    // 헬퍼 메서드
    private fun mockMaskedData(
        original: String,
        masked: String,
        isMasked: Boolean = false
    ) = mockk<com.goalmond.ai.domain.dto.MaskedData>().apply {
        every { this@apply.original } returns original
        every { this@apply.masked } returns masked
        every { this@apply.isMasked() } returns isMasked
        every { this@apply.tokens } returns emptyMap()
    }
}
