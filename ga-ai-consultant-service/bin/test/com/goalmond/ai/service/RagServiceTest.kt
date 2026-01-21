package com.goalmond.ai.service

import com.goalmond.ai.domain.entity.Document
import com.goalmond.ai.repository.DocumentRepository
import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.output.Response
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

/**
 * RagService 테스트
 * 
 * RAG 문서 검색 및 임베딩 생성 기능을 검증합니다.
 */
class RagServiceTest {

    private lateinit var ragService: RagService
    private lateinit var documentRepository: DocumentRepository
    private lateinit var embeddingModel: EmbeddingModel

    @BeforeEach
    fun setUp() {
        documentRepository = mockk()
        embeddingModel = mockk()
        ragService = RagService(documentRepository, embeddingModel)
    }

    @Test
    fun `유사 문서 검색 테스트`() {
        // Given
        val query = "미국 유학 정보"
        val tenantId = "tenant-a"
        val mockEmbedding = mockk<Embedding>()
        val mockDocuments = listOf(
            Document(
                title = "미국 대학 입학 가이드",
                content = "미국 대학 입학에 필요한 정보...",
                tenantId = tenantId
            )
        )
        
        every { mockEmbedding.vector() } returns floatArrayOf(0.1f, 0.2f, 0.3f)
        every { mockEmbedding.dimension() } returns 3
        every { embeddingModel.embed(query) } returns Response.from(mockEmbedding)
        every { 
            documentRepository.findSimilarDocuments(any(), eq(tenantId), eq(5)) 
        } returns mockDocuments
        
        // When
        val results = ragService.searchSimilarDocuments(query, tenantId, 5)
        
        // Then
        assertEquals(1, results.size)
        assertEquals("미국 대학 입학 가이드", results[0].title)
        verify { embeddingModel.embed(query) }
        verify { documentRepository.findSimilarDocuments(any(), tenantId, 5) }
    }

    @Test
    fun `공개 문서 검색 테스트`() {
        // Given
        val query = "유학 비자 정보"
        val mockEmbedding = mockk<Embedding>()
        val mockDocuments = listOf(
            Document(
                title = "유학 비자 가이드",
                content = "유학 비자 신청 방법...",
                tenantId = null
            )
        )
        
        every { mockEmbedding.vector() } returns floatArrayOf(0.1f, 0.2f)
        every { mockEmbedding.dimension() } returns 2
        every { embeddingModel.embed(query) } returns Response.from(mockEmbedding)
        every { 
            documentRepository.findSimilarPublicDocuments(any(), eq(5)) 
        } returns mockDocuments
        
        // When
        val results = ragService.searchSimilarDocuments(query, null, 5)
        
        // Then
        assertEquals(1, results.size)
        assertNull(results[0].tenantId)
        verify { documentRepository.findSimilarPublicDocuments(any(), 5) }
    }

    @Test
    fun `LLM 컨텍스트 포맷팅 테스트`() {
        // Given
        val documents = listOf(
            Document(
                title = "문서 1",
                content = "내용 1",
                sourceUrl = "https://example.com/1"
            ),
            Document(
                title = "문서 2",
                content = "내용 2"
            )
        )
        
        // When
        val context = ragService.formatContextForLlm(documents)
        
        // Then
        assertTrue(context.contains("문서 1"))
        assertTrue(context.contains("문서 2"))
        assertTrue(context.contains("내용 1"))
        assertTrue(context.contains("내용 2"))
        assertTrue(context.contains("https://example.com/1"))
        assertTrue(context.contains("관련 지식 베이스 정보"))
    }

    @Test
    fun `빈 문서 리스트 포맷팅 테스트`() {
        // Given
        val documents = emptyList<Document>()
        
        // When
        val context = ragService.formatContextForLlm(documents)
        
        // Then
        assertEquals("관련 문서를 찾을 수 없습니다.", context)
    }

    @Test
    fun `문서 임베딩 생성 테스트`() {
        // Given
        val document = Document(
            title = "테스트 문서",
            content = "테스트 내용",
            tenantId = "tenant-a"
        )
        val mockEmbedding = mockk<Embedding>()
        
        every { mockEmbedding.vector() } returns floatArrayOf(0.1f, 0.2f, 0.3f)
        every { mockEmbedding.dimension() } returns 3
        every { embeddingModel.embed(document.content) } returns Response.from(mockEmbedding)
        every { documentRepository.save(any()) } returns document
        
        // When
        val result = ragService.createAndSaveEmbedding(document)
        
        // Then
        assertNotNull(result.embedding)
        assertEquals("[0.1,0.2,0.3]", result.embedding)
        verify { embeddingModel.embed(document.content) }
        verify { documentRepository.save(any()) }
    }

    @Test
    fun `문서 통계 조회 테스트`() {
        // Given
        val tenantId = "tenant-a"
        val mockDocuments = listOf(
            Document(title = "Doc 1", content = "Content 1", tenantId = tenantId),
            Document(title = "Doc 2", content = "Content 2", tenantId = tenantId)
        )
        
        every { documentRepository.findByTenantId(tenantId) } returns mockDocuments
        
        // When
        val stats = ragService.getDocumentStatistics(tenantId)
        
        // Then
        assertEquals(2, stats.totalDocuments)
        assertEquals(tenantId, stats.tenantId)
    }
}
