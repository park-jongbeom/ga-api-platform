package com.goalmond.api.config.ai

import com.goalmond.api.service.ai.GeminiClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.ai.document.Document
import org.springframework.ai.embedding.EmbeddingRequest

/**
 * CustomGeminiEmbeddingClient 단위 테스트 (GeminiClient 모킹).
 */
class CustomGeminiEmbeddingClientTest {

    /** -1..1 범위의 테스트 임베딩 벡터 */
    private fun makeEmbedding(size: Int = 768): List<Double> =
        (1..size).map { 0.001 * it }.toList()

    @Test
    fun `embed String은 768차원 FloatArray 반환`() {
        val geminiClient = org.mockito.kotlin.mock<GeminiClient> {
            on { embedContent("테스트") }.thenReturn(makeEmbedding())
        }
        val client = CustomGeminiEmbeddingClient(geminiClient)

        val result = client.embed("테스트")

        assertThat(result).hasSize(768)
    }

    @Test
    fun `embed Document는 768차원 FloatArray 반환`() {
        val geminiClient = org.mockito.kotlin.mock<GeminiClient> {
            on { embedContent("문서 내용") }.thenReturn(makeEmbedding())
        }
        val client = CustomGeminiEmbeddingClient(geminiClient)

        val doc = Document("문서 내용")
        val result = client.embed(doc)

        assertThat(result).hasSize(768)
    }

    @Test
    fun `embed List는 List of FloatArray 반환`() {
        val geminiClient = org.mockito.kotlin.mock<GeminiClient> {
            on { embedContent(org.mockito.kotlin.any()) }.thenReturn(makeEmbedding())
        }
        val client = CustomGeminiEmbeddingClient(geminiClient)

        val result = client.embed(listOf("a", "b"))

        assertThat(result).hasSize(2)
        assertThat(result[0]).hasSize(768)
        assertThat(result[1]).hasSize(768)
    }

    @Test
    fun `call EmbeddingRequest는 EmbeddingResponse 반환`() {
        val geminiClient = org.mockito.kotlin.mock<GeminiClient> {
            on { embedContent(org.mockito.kotlin.any()) }.thenReturn(makeEmbedding())
        }
        val client = CustomGeminiEmbeddingClient(geminiClient)

        val request = EmbeddingRequest(listOf("텍스트1"), null)
        val response = client.call(request)

        assertThat(response).isNotNull
        assertThat(response.results).hasSize(1)
    }

    @Test
    fun `dimensions는 768 반환`() {
        val geminiClient = org.mockito.kotlin.mock<GeminiClient>()
        val client = CustomGeminiEmbeddingClient(geminiClient)

        assertThat(client.dimensions()).isEqualTo(768)
    }
}
