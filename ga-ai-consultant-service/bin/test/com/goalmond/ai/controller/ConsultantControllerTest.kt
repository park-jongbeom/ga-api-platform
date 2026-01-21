package com.goalmond.ai.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.goalmond.ai.config.RateLimitConfig
import com.goalmond.ai.domain.dto.ConsultantRequest
import com.goalmond.ai.domain.dto.CreateConversationRequest
import com.goalmond.ai.domain.entity.Conversation
import com.goalmond.ai.service.ChatResponse
import com.goalmond.ai.service.ConsultantService
import com.goalmond.ai.security.filter.TenantContext
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.util.*

/**
 * ConsultantController 테스트
 * 
 * REST API 엔드포인트를 검증합니다.
 */
@WebMvcTest(ConsultantController::class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Import(ConsultantControllerTest.TestConfig::class)
class ConsultantControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc
    
    @Autowired
    private lateinit var consultantService: ConsultantService
    
    @Autowired
    private lateinit var rateLimitConfig: RateLimitConfig
    
    @Autowired
    private lateinit var objectMapper: ObjectMapper
    
    private val tenantId = "tenant-a"

    @TestConfiguration
    class TestConfig {
        @Bean
        fun consultantService(): ConsultantService = mockk()
        
        @Bean
        fun rateLimitConfig(): RateLimitConfig = mockk()
    }

    @BeforeEach
    fun setUp() {
        TenantContext.setTenantId(tenantId)
    }

    @Test
    @WithMockUser(username = "user-123", roles = ["USER"])
    fun `AI 상담 API 성공 테스트`() {
        // Given
        val conversationId = UUID.randomUUID()
        val request = ConsultantRequest(
            message = "미국 유학에 대해 알려주세요.",
            conversationId = conversationId
        )
        
        val chatResponse = ChatResponse(
            response = "미국 유학은...",
            conversationId = conversationId,
            hasSensitiveData = false,
            relevantDocumentsCount = 3
        )
        
        every { rateLimitConfig.resolveBucket(any()) } returns mockk {
            every { tryConsume(1) } returns true
        }
        every { 
            consultantService.processChat(
                conversationId = conversationId,
                userMessage = request.message,
                userId = "user-123",
                tenantId = tenantId
            )
        } returns chatResponse
        
        // When & Then
        mockMvc.perform(
            post("/api/ai/consultant/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.response").value("미국 유학은..."))
            .andExpect(jsonPath("$.data.conversationId").value(conversationId.toString()))
            .andExpect(jsonPath("$.data.hasSensitiveData").value(false))
            .andExpect(jsonPath("$.data.relevantDocumentsCount").value(3))
        
        verify { 
            consultantService.processChat(
                conversationId = conversationId,
                userMessage = request.message,
                userId = "user-123",
                tenantId = tenantId
            )
        }
    }

    @Test
    @WithMockUser(username = "user-123", roles = ["USER"])
    fun `새 대화 세션 생성 API 테스트`() {
        // Given
        val request = CreateConversationRequest(title = "유학 상담")
        val conversation = Conversation(
            userId = "user-123",
            tenantId = tenantId,
            title = "유학 상담"
        ).apply {
            id = UUID.randomUUID()
        }
        
        every { 
            consultantService.createConversation("user-123", tenantId, "유학 상담")
        } returns conversation
        
        // When & Then
        mockMvc.perform(
            post("/api/ai/consultant/conversations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(conversation.id.toString()))
            .andExpect(jsonPath("$.data.title").value("유학 상담"))
    }

    @Test
    @WithMockUser(username = "user-123", roles = ["USER"])
    fun `입력값 검증 실패 테스트`() {
        // Given
        val request = ConsultantRequest(message = "") // 빈 메시지
        
        // When & Then
        mockMvc.perform(
            post("/api/ai/consultant/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @WithMockUser(username = "user-123", roles = ["USER"])
    fun `최대 길이 초과 테스트`() {
        // Given
        val longMessage = "a".repeat(2001) // 2000자 초과
        val request = ConsultantRequest(message = longMessage)
        
        // When & Then
        mockMvc.perform(
            post("/api/ai/consultant/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `인증 없이 API 호출 시 401 테스트`() {
        // Given
        val request = ConsultantRequest(message = "테스트")
        
        // When & Then
        mockMvc.perform(
            post("/api/ai/consultant/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `헬스 체크 API 테스트`() {
        // When & Then
        mockMvc.perform(get("/api/ai/consultant/health"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("UP"))
    }
}
