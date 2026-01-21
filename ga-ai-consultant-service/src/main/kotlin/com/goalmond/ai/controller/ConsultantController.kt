package com.goalmond.ai.controller

import com.goalmond.ai.config.RateLimitConfig
import com.goalmond.ai.config.RateLimitExceededException
import com.goalmond.ai.domain.dto.*
import com.goalmond.ai.security.filter.TenantContext
import com.goalmond.ai.service.ConsultantService
import com.goalmond.common.dto.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.util.*

/**
 * AI 상담 컨트롤러
 * 
 * AI 상담 API 엔드포인트를 제공합니다.
 * 
 * 보안:
 * - JWT 인증 필수
 * - Rate Limiting 적용
 * - 테넌트 격리
 * - 입력값 검증
 */
@RestController
@RequestMapping("/api/ai/consultant")
@Tag(name = "AI Consultant", description = "AI 상담 API")
@SecurityRequirement(name = "Bearer Authentication")
class ConsultantController(
    private val consultantService: ConsultantService,
    private val rateLimitConfig: RateLimitConfig
) {
    
    private val logger = LoggerFactory.getLogger(javaClass)
    
    /**
     * AI 상담 처리
     * 
     * POST /api/ai/consultant/chat
     */
    @PostMapping("/chat")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "AI 상담",
        description = "사용자 메시지를 받아 AI 상담 응답을 생성합니다. 민감정보는 자동으로 마스킹됩니다."
    )
    fun chat(
        @Valid @RequestBody request: ConsultantRequest,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<ConsultantResponse>> {
        
        val userId = authentication.name
        val tenantId = TenantContext.requireTenantId()
        
        // 1. Rate Limiting 검증
        val bucket = rateLimitConfig.resolveBucket(userId)
        if (!bucket.tryConsume(1)) {
            logger.warn("Rate limit 초과: userId=$userId")
            throw RateLimitExceededException("요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요.")
        }
        
        // 2. 대화 세션 ID 처리 (없으면 새로 생성)
        val conversationId = request.conversationId 
            ?: consultantService.createConversation(userId, tenantId).id!!
        
        // 3. AI 상담 처리
        val chatResponse = consultantService.processChat(
            conversationId = conversationId,
            userMessage = request.message,
            userId = userId,
            tenantId = tenantId
        )
        
        // 4. 응답 변환
        val response = ConsultantResponse(
            response = chatResponse.response,
            conversationId = chatResponse.conversationId,
            hasSensitiveData = chatResponse.hasSensitiveData,
            relevantDocumentsCount = chatResponse.relevantDocumentsCount
        )
        
        logger.info("AI 상담 완료: userId=$userId, conversationId=$conversationId")
        
        return ResponseEntity.ok(ApiResponse.success(response))
    }
    
    /**
     * 새 대화 세션 생성
     * 
     * POST /api/ai/consultant/conversations
     */
    @PostMapping("/conversations")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "새 대화 세션 생성",
        description = "새로운 AI 상담 세션을 생성합니다."
    )
    fun createConversation(
        @Valid @RequestBody request: CreateConversationRequest,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<ConversationResponse>> {
        
        val userId = authentication.name
        val tenantId = TenantContext.requireTenantId()
        
        val conversation = consultantService.createConversation(
            userId = userId,
            tenantId = tenantId,
            title = request.title
        )
        
        val response = ConversationResponse.from(conversation)
        
        logger.info("새 대화 세션 생성: userId=$userId, conversationId=${conversation.id}")
        
        return ResponseEntity.ok(ApiResponse.success(response))
    }
    
    /**
     * 대화 내역 조회
     * 
     * GET /api/ai/consultant/conversations/{conversationId}
     */
    @GetMapping("/conversations/{conversationId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "대화 내역 조회",
        description = "특정 대화 세션의 전체 내역을 조회합니다."
    )
    fun getConversationHistory(
        @PathVariable conversationId: UUID,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<ConversationHistoryResponse>> {
        
        val tenantId = TenantContext.requireTenantId()
        
        val messages = consultantService.getConversationHistory(conversationId, tenantId)
        val conversation = messages.firstOrNull()?.conversation
            ?: throw IllegalArgumentException("대화 세션을 찾을 수 없습니다.")
        
        val response = ConversationHistoryResponse(
            conversation = ConversationResponse.from(conversation),
            messages = messages.map { MessageResponse.from(it) }
        )
        
        return ResponseEntity.ok(ApiResponse.success(response))
    }
    
    /**
     * 헬스 체크
     * 
     * GET /api/ai/consultant/health
     */
    @GetMapping("/health")
    @Operation(
        summary = "헬스 체크",
        description = "서비스 상태를 확인합니다."
    )
    fun healthCheck(): ResponseEntity<ApiResponse<Map<String, String>>> {
        val health = mapOf(
            "status" to "UP",
            "service" to "AI Consultant Service"
        )
        return ResponseEntity.ok(ApiResponse.success(health))
    }
}
