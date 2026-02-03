package com.goalmond.api.service.ai

import com.goalmond.api.config.GeminiProperties
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import kotlin.math.min
import kotlin.math.pow

/**
 * Gemini AI HTTP 클라이언트 (GAM-3, Phase 1).
 * 
 * 기능:
 * 1. generateContent: 텍스트 생성 (설명 생성용)
 * 2. embedContent: 텍스트 임베딩 (RAG 검색용, 768차원)
 * 
 * Gemini API 문서: https://ai.google.dev/api/rest
 */
@Service
class GeminiClient(
    private val restClient: RestClient,
    private val geminiProperties: GeminiProperties
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    companion object {
        private const val MAX_RETRIES = 3
        private const val INITIAL_BACKOFF_MS = 1000L
    }
    
    /**
     * 텍스트 생성 API (Explainable AI용).
     * 
     * @param prompt 프롬프트 텍스트
     * @return 생성된 텍스트
     * @throws GeminiApiException API 호출 실패 시
     */
    fun generateContent(prompt: String): String {
        return executeWithRetry("generateContent") {
            val request = mapOf(
                "contents" to listOf(
                    mapOf(
                        "parts" to listOf(
                            mapOf("text" to prompt)
                        )
                    )
                )
            )
            
            val response = restClient.post()
                .uri("/models/${geminiProperties.model}:generateContent")
                .header("x-goog-api-key", geminiProperties.apiKey)
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError) { _, resp ->
                    throw GeminiApiException("Client error: ${resp.statusCode}")
                }
                .onStatus(HttpStatusCode::is5xxServerError) { _, resp ->
                    throw GeminiApiException("Server error: ${resp.statusCode}")
                }
                .body(Map::class.java) as Map<*, *>
            
            extractTextFromResponse(response)
        }
    }
    
    /**
     * 텍스트 임베딩 API (RAG 검색용).
     * 
     * @param text 임베딩할 텍스트
     * @return 768차원 벡터
     * @throws GeminiApiException API 호출 실패 시
     */
    fun embedContent(text: String): List<Double> {
        return executeWithRetry("embedContent") {
            // 공식 스펙: model(본문), content.parts, outputDimensionality(camelCase)
            val modelInBody = "models/${geminiProperties.embeddingModel}"
            val request = mapOf(
                "model" to modelInBody,
                "content" to mapOf(
                    "parts" to listOf(
                        mapOf("text" to text)
                    )
                ),
                "outputDimensionality" to 768  // 768차원 (gemini-embedding-001 지원)
            )
            
            val response = restClient.post()
                .uri("/models/${geminiProperties.embeddingModel}:embedContent")
                .header("x-goog-api-key", geminiProperties.apiKey)
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError) { _, resp ->
                    val body = String(resp.body.readAllBytes(), Charsets.UTF_8)
                    throw GeminiApiException("Client error: ${resp.statusCode} - $body")
                }
                .onStatus(HttpStatusCode::is5xxServerError) { _, resp ->
                    val body = String(resp.body.readAllBytes(), Charsets.UTF_8)
                    throw GeminiApiException("Server error: ${resp.statusCode} - $body")
                }
                .body(Map::class.java) as Map<*, *>
            
            extractEmbeddingFromResponse(response)
        }
    }
    
    /**
     * 재시도 로직 (Exponential Backoff).
     */
    private fun <T> executeWithRetry(operation: String, block: () -> T): T {
        var lastException: Exception? = null
        
        repeat(MAX_RETRIES) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                lastException = e
                val backoffMs = INITIAL_BACKOFF_MS * 2.0.pow(attempt.toDouble()).toLong()
                
                logger.warn(
                    "Gemini API $operation failed (attempt ${attempt + 1}/$MAX_RETRIES), " +
                    "retrying in ${backoffMs}ms: ${e.message}"
                )
                
                if (attempt < MAX_RETRIES - 1) {
                    Thread.sleep(backoffMs)
                }
            }
        }
        
        val causeMsg = lastException?.message ?: lastException?.javaClass?.simpleName ?: "unknown"
        throw GeminiApiException(
            "Gemini API $operation failed after $MAX_RETRIES attempts: $causeMsg",
            lastException
        )
    }
    
    /**
     * Gemini 응답에서 생성된 텍스트 추출.
     */
    private fun extractTextFromResponse(response: Map<*, *>): String {
        val candidates = response["candidates"] as? List<*>
            ?: throw GeminiApiException("No candidates in response")
        
        val firstCandidate = candidates.firstOrNull() as? Map<*, *>
            ?: throw GeminiApiException("Empty candidates")
        
        val content = firstCandidate["content"] as? Map<*, *>
            ?: throw GeminiApiException("No content in candidate")
        
        val parts = content["parts"] as? List<*>
            ?: throw GeminiApiException("No parts in content")
        
        val firstPart = parts.firstOrNull() as? Map<*, *>
            ?: throw GeminiApiException("Empty parts")
        
        return firstPart["text"] as? String
            ?: throw GeminiApiException("No text in part")
    }
    
    /**
     * Gemini 응답에서 임베딩 벡터 추출.
     */
    private fun extractEmbeddingFromResponse(response: Map<*, *>): List<Double> {
        val embedding = response["embedding"] as? Map<*, *>
            ?: throw GeminiApiException("No embedding in response")
        
        val values = embedding["values"] as? List<*>
            ?: throw GeminiApiException("No values in embedding")
        
        return values.map { value ->
            when (value) {
                is Double -> value
                is Number -> value.toDouble()
                else -> throw GeminiApiException("Invalid embedding value type: ${value?.javaClass}")
            }
        }
    }
}

/**
 * Gemini API 예외.
 */
class GeminiApiException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
