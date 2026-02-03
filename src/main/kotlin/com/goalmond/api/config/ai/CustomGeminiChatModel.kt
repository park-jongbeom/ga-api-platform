package com.goalmond.api.config.ai

import com.goalmond.api.service.ai.GeminiClient
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux

/**
 * Spring AI ChatModel 인터페이스를 구현한 Custom Gemini 래퍼.
 * 
 * 무료 Google AI Studio API (generativelanguage.googleapis.com)를 사용합니다.
 * Vertex AI가 아닌 직접 REST API 호출 방식이므로 Custom 구현이 필요합니다.
 * 
 * 특징:
 * - 기존 GeminiClient를 재사용하여 비용 절감
 * - Spring AI의 표준 인터페이스 제공
 * - 나중에 Vertex AI로 교체 쉬움 (이 클래스만 변경)
 */
@Component
class CustomGeminiChatModel(
    private val geminiClient: GeminiClient
) : ChatModel {
    
    /**
     * 동기 호출.
     * Prompt에서 사용자 메시지를 추출하여 GeminiClient로 전달.
     */
    override fun call(prompt: Prompt): ChatResponse {
        // Prompt에서 첫 번째 메시지 추출
        val userMessage = prompt.instructions.firstOrNull()?.content 
            ?: throw IllegalArgumentException("Prompt must contain at least one message")
        
        // 기존 GeminiClient 호출 (무료 API)
        val response = geminiClient.generateContent(userMessage)
        
        // Spring AI ChatResponse로 변환
        return ChatResponse(
            listOf(
                Generation(AssistantMessage(response))
            )
        )
    }
    
    /**
     * 스트리밍 호출.
     * 
     * 무료 Gemini API는 스트리밍을 지원하지 않으므로 UnsupportedOperationException.
     * Vertex AI로 전환 시 구현 가능.
     */
    override fun stream(prompt: Prompt): Flux<ChatResponse> {
        throw UnsupportedOperationException(
            "Streaming not supported for free Gemini API. " +
            "Use Vertex AI for streaming support."
        )
    }
    
    override fun getDefaultOptions(): org.springframework.ai.chat.prompt.ChatOptions {
        return DefaultChatOptions
    }
}

/** ChatModel.getDefaultOptions()용 기본 옵션 (모든 값 null). */
private object DefaultChatOptions : org.springframework.ai.chat.prompt.ChatOptions {
    override fun getModel(): String? = null
    override fun getTemperature(): Double? = null
    override fun getTopK(): Int? = null
    override fun getTopP(): Double? = null
    override fun getMaxTokens(): Int? = null
    override fun getFrequencyPenalty(): Double? = null
    override fun getPresencePenalty(): Double? = null
    override fun getStopSequences(): List<String>? = null
    override fun copy(): org.springframework.ai.chat.prompt.ChatOptions = this
}
