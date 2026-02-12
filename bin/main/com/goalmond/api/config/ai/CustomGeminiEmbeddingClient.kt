package com.goalmond.api.config.ai

import com.goalmond.api.service.ai.GeminiClient
import org.springframework.ai.document.Document
import org.springframework.ai.embedding.Embedding
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.embedding.EmbeddingRequest
import org.springframework.ai.embedding.EmbeddingResponse
import org.springframework.stereotype.Component

/**
 * Spring AI EmbeddingModel 인터페이스를 구현한 Custom Gemini 래퍼.
 * 
 * 무료 Google AI Studio API (generativelanguage.googleapis.com)를 사용합니다.
 * Vertex AI가 아닌 직접 REST API 호출 방식이므로 Custom 구현이 필요합니다.
 * 
 * 특징:
 * - 기존 GeminiClient를 재사용하여 비용 절감
 * - Gemini text-embedding-004 모델 (768차원)
 * - Spring AI의 표준 인터페이스 제공
 * - Rate Limiting 고려 (60 req/min)
 */
@Component
class CustomGeminiEmbeddingClient(
    private val geminiClient: GeminiClient
) : EmbeddingModel {
    
    companion object {
        const val DIMENSIONS = 768  // Gemini text-embedding-004
    }
    
    private fun List<Double>.toFloatArray(): FloatArray =
        map { it.toFloat() }.toFloatArray()
    
    /**
     * 단일 Document 임베딩.
     */
    override fun embed(document: Document): FloatArray {
        return geminiClient.embedContent(document.content).toFloatArray()
    }
    
    /**
     * 단일 텍스트 임베딩.
     */
    override fun embed(text: String): FloatArray {
        return geminiClient.embedContent(text).toFloatArray()
    }
    
    /**
     * 배치 텍스트 임베딩.
     * 
     * Rate Limiting 주의: 무료 API는 60 req/min 제한.
     * 각 요청 사이에 1초 대기 (안전하게 60 req/min 이하 유지).
     */
    override fun embed(texts: List<String>): List<FloatArray> {
        return texts.mapIndexed { index, text ->
            val embedding = geminiClient.embedContent(text).toFloatArray()
            if (index < texts.size - 1) {
                Thread.sleep(1000)
            }
            embedding
        }
    }
    
    /**
     * EmbeddingRequest 처리.
     * 
     * Spring AI의 표준 요청 형식을 처리합니다.
     */
    override fun call(request: EmbeddingRequest): EmbeddingResponse {
        val embeddings = embed(request.instructions)
        return EmbeddingResponse(
            embeddings.mapIndexed { index, vector ->
                Embedding(vector, index)
            }
        )
    }
    
    /**
     * 임베딩 차원 수 반환.
     */
    override fun dimensions(): Int = DIMENSIONS
}
