package com.goalmond.api.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

/**
 * Gemini AI 설정 (GAM-3, Phase 1).
 * 텍스트 생성 및 임베딩 API 연동.
 */
@Configuration
class GeminiConfig {
    
    @Bean
    @ConfigurationProperties(prefix = "app.ai.gemini")
    fun geminiProperties(): GeminiProperties = GeminiProperties()
    
    @Bean
    fun geminiRestClient(geminiProperties: GeminiProperties): RestClient {
        return RestClient.builder()
            .baseUrl(geminiProperties.apiUrl)
            .defaultHeader("Content-Type", "application/json")
            .build()
    }
}

/**
 * Gemini API 설정 속성.
 */
data class GeminiProperties(
    var apiKey: String = "",
    var model: String = "gemini-2.0-flash",
    var embeddingModel: String = "gemini-embedding-001",
    var apiUrl: String = "https://generativelanguage.googleapis.com/v1beta/models"
)
