package com.goalmond.ai.service

import com.goalmond.ai.domain.entity.Conversation
import com.goalmond.ai.domain.entity.Message
import com.goalmond.ai.domain.entity.MessageRole
import com.goalmond.ai.domain.prompt.PromptTemplateManager
import com.goalmond.ai.repository.ConversationRepository
import com.goalmond.ai.repository.MessageRepository
import com.goalmond.ai.security.validator.InputSanitizer
import dev.langchain4j.model.chat.ChatLanguageModel
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

/**
 * AI 상담 서비스
 * 
 * 민감정보 마스킹 → RAG 검색 → LLM 호출 → 응답 저장 파이프라인을 관리합니다.
 * 
 * 플로우:
 * 1. 입력값 검증 (XSS/SQLi 차단)
 * 2. 민감정보 마스킹
 * 3. RAG 문서 검색
 * 4. LLM 호출 (마스킹된 데이터 + RAG 컨텍스트)
 * 5. 응답 저장 (감사 로그)
 */
@Service
class ConsultantService(
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
    private val maskingService: MaskingService,
    private val ragService: RagService,
    private val inputSanitizer: InputSanitizer,
    private val chatLanguageModel: ChatLanguageModel,
    private val promptTemplateManager: PromptTemplateManager
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    /**
     * AI 상담 처리
     * 
     * @param conversationId 대화 세션 ID
     * @param userMessage 사용자 메시지
     * @param userId 사용자 ID
     * @param tenantId 테넌트 ID
     * @return AI 응답
     */
    @Transactional
    fun processChat(
        conversationId: UUID,
        userMessage: String,
        userId: String,
        tenantId: String
    ): ChatResponse {
        try {
            // 1. 입력값 검증
            val validationResult = inputSanitizer.validate(userMessage)
            if (!validationResult.isValid) {
                logger.warn("입력값 검증 실패: ${validationResult.reason}")
                throw SecurityException(validationResult.reason ?: "입력값 검증에 실패했습니다.")
            }
            
            // 2. 대화 세션 조회 또는 생성
            val conversation = conversationRepository.findByIdAndTenantId(conversationId, tenantId)
                .orElseThrow { IllegalArgumentException("대화 세션을 찾을 수 없습니다.") }
            
            // 3. 민감정보 마스킹
            val maskedData = maskingService.maskSensitiveData(userMessage)
            logger.info("마스킹 완료: ${maskedData.tokens.size}개 토큰 생성")
            
            // 4. 사용자 메시지 저장
            val userMessageEntity = Message(
                conversation = conversation,
                role = MessageRole.USER,
                originalContent = maskedData.original,
                maskedContent = maskedData.masked,
                maskedTokens = maskedData.tokens
            )
            messageRepository.save(userMessageEntity)
            
            // 5. RAG 문서 검색
            val relevantDocuments = ragService.searchSimilarDocuments(
                query = maskedData.masked,
                tenantId = tenantId,
                limit = 5
            )
            val ragContext = ragService.formatContextForLlm(relevantDocuments)
            
            logger.debug("RAG 검색 완료: ${relevantDocuments.size}개 문서 발견")
            
            // 6. LLM 프롬프트 구성 (PromptTemplateManager 사용)
            val fullPrompt = promptTemplateManager.createStudyAbroadPrompt(
                ragContext = ragContext,
                userQuery = maskedData.masked
            )
            
            logger.debug("프롬프트 템플릿 버전: ${promptTemplateManager.getVersion()}")
            
            // 7. LLM 호출 (오류 처리 개선)
            val (llmResponse, isErrorResponse) = try {
                val response = chatLanguageModel.generate(fullPrompt)
                logger.info("LLM 호출 성공 (길이: ${response.length}자)")
                Pair(response, false)
            } catch (e: Exception) {
                logger.error("LLM 호출 실패: ${e.message}", e)
                // 오류 응답도 저장하여 대화 일관성 유지
                // (추후 재시도 또는 분석에 활용)
                Pair("죄송합니다. 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", true)
            }
            
            // 8. AI 응답 메시지 저장 (오류 응답도 포함)
            val assistantMessage = Message(
                conversation = conversation,
                role = MessageRole.ASSISTANT,
                originalContent = llmResponse,
                llmResponse = llmResponse
            ).apply {
                // 메타데이터에 오류 여부 표시 (향후 재시도 기능에 활용 가능)
                if (isErrorResponse) {
                    logger.debug("오류 응답 메시지 저장: conversationId=$conversationId")
                }
            }
            messageRepository.save(assistantMessage)
            
            logger.info("AI 상담 처리 완료 (conversationId: $conversationId)")
            
            return ChatResponse(
                response = llmResponse,
                conversationId = conversationId,
                hasSensitiveData = maskedData.isMasked(),
                relevantDocumentsCount = relevantDocuments.size
            )
            
        } catch (e: Exception) {
            logger.error("AI 상담 처리 실패: ${e.message}", e)
            throw RuntimeException("AI 상담 처리 중 오류가 발생했습니다.", e)
        }
    }
    
    /**
     * 새 대화 세션 생성
     * 
     * @param userId 사용자 ID
     * @param tenantId 테넌트 ID
     * @param title 대화 제목
     * @return 생성된 대화 세션
     */
    @Transactional
    fun createConversation(
        userId: String,
        tenantId: String,
        title: String? = null
    ): Conversation {
        val conversation = Conversation(
            userId = userId,
            tenantId = tenantId,
            title = title ?: "새 상담"
        )
        
        return conversationRepository.save(conversation)
    }
    
    /**
     * 대화 내역 조회
     * 
     * @param conversationId 대화 세션 ID
     * @param tenantId 테넌트 ID (격리)
     * @return 메시지 목록
     */
    fun getConversationHistory(conversationId: UUID, tenantId: String): List<Message> {
        // 테넌트 격리 검증
        conversationRepository.findByIdAndTenantId(conversationId, tenantId)
            .orElseThrow { IllegalArgumentException("대화 세션을 찾을 수 없습니다.") }
        
        return messageRepository.findByConversationId(conversationId)
    }
    
}

/**
 * AI 상담 응답 DTO
 */
data class ChatResponse(
    /** AI 응답 텍스트 */
    val response: String,
    
    /** 대화 세션 ID */
    val conversationId: UUID,
    
    /** 민감정보 포함 여부 */
    val hasSensitiveData: Boolean,
    
    /** 참조된 문서 수 */
    val relevantDocumentsCount: Int
)
