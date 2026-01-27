package com.goalmond.ai.controller

import com.goalmond.ai.config.RateLimitConfig
import com.goalmond.ai.config.RateLimitExceededException
import com.goalmond.ai.domain.dto.*
import com.goalmond.ai.security.filter.TenantContext
import com.goalmond.ai.service.ConsultantService
import com.goalmond.common.dto.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
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
        description = """
            사용자 메시지를 받아 AI 상담 응답을 생성합니다.
            민감정보는 자동으로 마스킹됩니다.
            
            ## 인증
            - Authorization: Bearer {accessToken} 필수
            
            ## 에러 응답
            - 400: 요청 형식 오류
            - 401: 인증 실패
            - 429: Rate Limit 초과
        """
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "상담 응답 성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiResponse::class),
                    examples = [ExampleObject(
                        value = """
                            {
                              "success": true,
                              "data": {
                                "response": "추천 가능한 학교는 다음과 같습니다...",
                                "conversationId": "3f0c2e5e-4d9b-4c2d-9c10-1f2b3c4d5e6f",
                                "hasSensitiveData": false,
                                "relevantDocumentsCount": 2,
                                "timestamp": "2024-01-01T00:00:00"
                              },
                              "message": null,
                              "timestamp": "2024-01-01T00:00:00"
                            }
                        """
                    )]
                )]
            ),
            SwaggerApiResponse(responseCode = "400", description = "잘못된 요청"),
            SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
            SwaggerApiResponse(responseCode = "429", description = "요청 한도 초과"),
            SwaggerApiResponse(responseCode = "500", description = "서버 오류")
        ]
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
        description = """
            새로운 AI 상담 세션을 생성합니다.
            
            ## 인증
            - Authorization: Bearer {accessToken} 필수
        """
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "세션 생성 성공"),
            SwaggerApiResponse(responseCode = "400", description = "잘못된 요청"),
            SwaggerApiResponse(responseCode = "401", description = "인증 실패")
        ]
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
        description = """
            특정 대화 세션의 전체 내역을 조회합니다.
            
            ## 인증
            - Authorization: Bearer {accessToken} 필수
        """
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "조회 성공"),
            SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
            SwaggerApiResponse(responseCode = "404", description = "대화 세션 없음")
        ]
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
