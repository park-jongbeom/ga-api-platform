package com.goalmond.ai.config

import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

/**
 * LangChain4j 설정
 * 
 * Google Gemini 및 Embedding 모델을 설정합니다.
 * 
 * 참고:
 * - LangChain4j 공식 문서: https://docs.langchain4j.dev/
 * - Google AI Studio: https://aistudio.google.com/
 */
@Configuration
class LangChainConfig {
    
    /**
     * ChatLanguageModel 빈 등록
     * 
     * Google Gemini를 사용하여 대화형 AI 모델을 생성합니다.
     */
    @Bean
    fun chatLanguageModel(
        @Value("\${langchain4j.google-ai-gemini.api-key}") apiKey: String,
        @Value("\${langchain4j.google-ai-gemini.model-name}") modelName: String,
        @Value("\${langchain4j.google-ai-gemini.temperature}") temperature: Double,
        @Value("\${langchain4j.google-ai-gemini.max-tokens}") maxOutputTokens: Int,
        @Value("\${langchain4j.google-ai-gemini.timeout}") timeout: Duration
    ): ChatLanguageModel {
        return GoogleAiGeminiChatModel.builder()
            .apiKey(apiKey)
            .modelName(modelName)
            .temperature(temperature)
            .maxOutputTokens(maxOutputTokens)
            .timeout(timeout)
            .logRequestsAndResponses(true)
            .build()
    }
    
    /**
     * EmbeddingModel 빈 등록
     * 
     * Google Embedding 모델을 사용하여 텍스트 임베딩을 생성합니다.
     */
    @Bean
    fun embeddingModel(
        @Value("\${langchain4j.google-ai-gemini.api-key}") apiKey: String,
        @Value("\${langchain4j.google-ai-gemini.embedding-model}") modelName: String
    ): EmbeddingModel {
        return GoogleAiEmbeddingModel.builder()
            .apiKey(apiKey)
            .modelName(modelName)
            .timeout(Duration.ofSeconds(30))
            .logRequestsAndResponses(true)
            .build()
    }
}
