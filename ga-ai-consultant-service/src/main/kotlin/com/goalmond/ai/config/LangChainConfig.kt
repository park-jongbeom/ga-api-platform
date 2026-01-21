package com.goalmond.ai.config

import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.openai.OpenAiChatModel
import dev.langchain4j.model.openai.OpenAiEmbeddingModel
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

/**
 * LangChain4j 설정
 * 
 * OpenAI GPT-4 및 Embedding 모델을 설정합니다.
 * 
 * 참고:
 * - LangChain4j 공식 문서: https://docs.langchain4j.dev/
 * - OpenAI API: https://platform.openai.com/docs/api-reference
 */
@Configuration
class LangChainConfig {
    
    /**
     * ChatLanguageModel 빈 등록
     * 
     * OpenAI GPT-4를 사용하여 대화형 AI 모델을 생성합니다.
     */
    @Bean
    fun chatLanguageModel(
        @Value("\${langchain4j.openai.api-key}") apiKey: String,
        @Value("\${langchain4j.openai.model-name}") modelName: String,
        @Value("\${langchain4j.openai.temperature}") temperature: Double,
        @Value("\${langchain4j.openai.max-tokens}") maxTokens: Int,
        @Value("\${langchain4j.openai.timeout}") timeout: Duration
    ): ChatLanguageModel {
        return OpenAiChatModel.builder()
            .apiKey(apiKey)
            .modelName(modelName)
            .temperature(temperature)
            .maxTokens(maxTokens)
            .timeout(timeout)
            .logRequests(true)
            .logResponses(true)
            .build()
    }
    
    /**
     * EmbeddingModel 빈 등록
     * 
     * OpenAI text-embedding-ada-002를 사용하여 텍스트 임베딩을 생성합니다.
     */
    @Bean
    fun embeddingModel(
        @Value("\${langchain4j.openai.api-key}") apiKey: String,
        @Value("\${langchain4j.embeddings.model-name}") modelName: String
    ): EmbeddingModel {
        return OpenAiEmbeddingModel.builder()
            .apiKey(apiKey)
            .modelName(modelName)
            .timeout(Duration.ofSeconds(30))
            .logRequests(true)
            .logResponses(true)
            .build()
    }
}
