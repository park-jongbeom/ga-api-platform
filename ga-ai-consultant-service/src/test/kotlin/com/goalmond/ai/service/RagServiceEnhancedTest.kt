package com.goalmond.ai.service

import com.goalmond.ai.domain.entity.Document
import com.goalmond.ai.repository.DocumentRepository
import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.output.Response
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * RagService 고도화 기능 테스트
 * 
 * 테스트 범위:
 * - 하이브리드 검색 (벡터 + 키워드)
 * - 출처 URL 검증
 * - 메타데이터 필터링
 * - 문서 컨텍스트 포맷팅
 * 
 * @author AI Consultant Team
 * @since 2026-01-21
 */
class RagServiceEnhancedTest {
    
    private lateinit var documentRepository: DocumentRepository
    private lateinit var embeddingModel: EmbeddingModel
    private lateinit var ragService: RagService
    
    @BeforeEach
    fun setup() {
        documentRepository = mockk()
        embeddingModel = mockk()
        ragService = RagService(documentRepository, embeddingModel)
    }
    
    @Test
    fun `하이브리드 검색 - 정상 동작`() {
        // Given
        val query = "미국 대학 입학 조건"
        val tenantId = "tenant-001"
        val embedding = mockk<Embedding>()
        val response = mockk<Response<Embedding>>()
        
        val documents = listOf(
            createMockDocument(
                title = "미국 대학 가이드",
                content = "TOEFL 80점 이상 필요",
                sourceUrl = "https://docs.goalmond.com/us-colleges"
            )
        )
        
        every { embeddingModel.embed(query) } returns response
        every { response.content() } returns embedding
        every { embedding.dimension() } returns 1536
        every { embedding.vector() } returns FloatArray(1536) { 0.1f }
        every { 
            documentRepository.findSimilarDocumentsHybrid(
                any(), 
                any(), 
                tenantId, 
                null, 
                5
            ) 
        } returns documents
        
        // When
        val results = ragService.searchSimilarDocuments(
            query = query,
            tenantId = tenantId,
            limit = 5,
            useHybrid = true
        )
        
        // Then
        assertEquals(1, results.size)
        assertEquals("미국 대학 가이드", results[0].title)
        
        verify(exactly = 1) { 
            documentRepository.findSimilarDocumentsHybrid(
                any(), 
                any(), 
                tenantId, 
                null, 
                5
            ) 
        }
    }
    
    @Test
    fun `하이브리드 검색 - 문서 타입 필터링`() {
        // Given
        val query = "유학 정보"
        val tenantId = "tenant-001"
        val documentType = "guide"
        val embedding = mockk<Embedding>()
        val response = mockk<Response<Embedding>>()
        
        every { embeddingModel.embed(query) } returns response
        every { response.content() } returns embedding
        every { embedding.dimension() } returns 1536
        every { embedding.vector() } returns FloatArray(1536) { 0.1f }
        every { 
            documentRepository.findSimilarDocumentsHybrid(
                any(), 
                any(), 
                tenantId, 
                documentType, 
                5
            ) 
        } returns emptyList()
        
        // When
        val results = ragService.searchSimilarDocuments(
            query = query,
            tenantId = tenantId,
            limit = 5,
            documentType = documentType,
            useHybrid = true
        )
        
        // Then
        assertTrue(results.isEmpty())
        
        verify(exactly = 1) { 
            documentRepository.findSimilarDocumentsHybrid(
                any(), 
                any(), 
                tenantId, 
                documentType, 
                5
            ) 
        }
    }
    
    @Test
    fun `벡터 단독 검색 - 하이브리드 비활성화`() {
        // Given
        val query = "유학 정보"
        val tenantId = "tenant-001"
        val embedding = mockk<Embedding>()
        val response = mockk<Response<Embedding>>()
        
        every { embeddingModel.embed(query) } returns response
        every { response.content() } returns embedding
        every { embedding.dimension() } returns 1536
        every { embedding.vector() } returns FloatArray(1536) { 0.1f }
        every { 
            documentRepository.findSimilarDocuments(any(), tenantId, 5) 
        } returns emptyList()
        
        // When
        val results = ragService.searchSimilarDocuments(
            query = query,
            tenantId = tenantId,
            limit = 5,
            useHybrid = false
        )
        
        // Then
        assertTrue(results.isEmpty())
        
        // 하이브리드가 아닌 기존 메서드 호출 확인
        verify(exactly = 1) { documentRepository.findSimilarDocuments(any(), tenantId, 5) }
        verify(exactly = 0) { documentRepository.findSimilarDocumentsHybrid(any(), any(), any(), any(), any()) }
    }
    
    @Test
    fun `공개 문서 하이브리드 검색`() {
        // Given
        val query = "공개 정보"
        val embedding = mockk<Embedding>()
        val response = mockk<Response<Embedding>>()
        
        val documents = listOf(
            createMockDocument(
                title = "공개 가이드",
                content = "누구나 볼 수 있는 정보"
            )
        )
        
        every { embeddingModel.embed(query) } returns response
        every { response.content() } returns embedding
        every { embedding.dimension() } returns 1536
        every { embedding.vector() } returns FloatArray(1536) { 0.1f }
        every { 
            documentRepository.findSimilarPublicDocumentsHybrid(any(), any(), 5) 
        } returns documents
        
        // When
        val results = ragService.searchSimilarDocuments(
            query = query,
            tenantId = null,
            limit = 5,
            useHybrid = true
        )
        
        // Then
        assertEquals(1, results.size)
        
        verify(exactly = 1) { documentRepository.findSimilarPublicDocumentsHybrid(any(), any(), 5) }
    }
    
    @Test
    fun `출처 검증 - 허용된 도메인`() {
        // Given
        val documents = listOf(
            createMockDocument(
                title = "문서1",
                content = "내용1",
                sourceUrl = "https://docs.goalmond.com/guide"
            ),
            createMockDocument(
                title = "문서2",
                content = "내용2",
                sourceUrl = "https://wiki.goalmond.com/policy"
            ),
            createMockDocument(
                title = "문서3",
                content = "내용3",
                sourceUrl = null  // 출처 없음 (허용)
            )
        )
        
        // When
        val context = ragService.formatContextForLlm(documents)
        
        // Then
        assertTrue(context.contains("문서1"))
        assertTrue(context.contains("문서2"))
        assertTrue(context.contains("문서3"))
        assertTrue(context.contains("docs.goalmond.com"))
        assertTrue(context.contains("wiki.goalmond.com"))
    }
    
    @Test
    fun `출처 검증 - 외부 도메인 차단`() {
        // Given
        val documents = listOf(
            createMockDocument(
                title = "악성 문서",
                content = "외부 내용",
                sourceUrl = "https://malicious.com/attack"
            ),
            createMockDocument(
                title = "정상 문서",
                content = "정상 내용",
                sourceUrl = "https://goalmond.com/guide"
            )
        )
        
        // When
        val context = ragService.formatContextForLlm(documents)
        
        // Then
        assertFalse(context.contains("악성 문서"))
        assertFalse(context.contains("malicious.com"))
        assertTrue(context.contains("정상 문서"))
        assertTrue(context.contains("goalmond.com"))
    }
    
    @Test
    fun `출처 검증 - 모든 문서 차단 시`() {
        // Given
        val documents = listOf(
            createMockDocument(
                title = "외부1",
                content = "내용1",
                sourceUrl = "https://evil.com/bad"
            ),
            createMockDocument(
                title = "외부2",
                content = "내용2",
                sourceUrl = "https://phishing.net/scam"
            )
        )
        
        // When
        val context = ragService.formatContextForLlm(documents)
        
        // Then
        assertEquals("관련 문서를 찾을 수 없습니다.", context)
    }
    
    @Test
    fun `컨텍스트 포맷팅 - 빈 문서 목록`() {
        // Given
        val documents = emptyList<Document>()
        
        // When
        val context = ragService.formatContextForLlm(documents)
        
        // Then
        assertEquals("관련 문서를 찾을 수 없습니다.", context)
    }
    
    @Test
    fun `컨텍스트 포맷팅 - 정상 동작`() {
        // Given
        val documents = listOf(
            createMockDocument(
                title = "문서1",
                content = "첫 번째 내용",
                sourceUrl = "https://goalmond.com/doc1"
            ),
            createMockDocument(
                title = "문서2",
                content = "두 번째 내용",
                sourceUrl = "https://internal.docs/doc2"
            )
        )
        
        // When
        val context = ragService.formatContextForLlm(documents)
        
        // Then
        assertTrue(context.contains("=== 관련 지식 베이스 정보 ==="))
        assertTrue(context.contains("【문서 1: 문서1】"))
        assertTrue(context.contains("첫 번째 내용"))
        assertTrue(context.contains("출처: https://goalmond.com/doc1"))
        assertTrue(context.contains("【문서 2: 문서2】"))
        assertTrue(context.contains("두 번째 내용"))
        assertTrue(context.contains("=== 위 정보를 참고하여 답변해주세요 ==="))
    }
    
    @Test
    fun `검색 실패 시 빈 목록 반환`() {
        // Given
        val query = "오류 발생"
        val tenantId = "tenant-001"
        
        every { embeddingModel.embed(query) } throws RuntimeException("임베딩 실패")
        
        // When
        val results = ragService.searchSimilarDocuments(
            query = query,
            tenantId = tenantId,
            limit = 5
        )
        
        // Then
        assertTrue(results.isEmpty())
    }
    
    @Test
    fun `문서 통계 조회 - 테넌트별`() {
        // Given
        val tenantId = "tenant-001"
        val documents = listOf(
            createMockDocument("문서1", "내용1"),
            createMockDocument("문서2", "내용2"),
            createMockDocument("문서3", "내용3")
        )
        
        every { documentRepository.findByTenantId(tenantId) } returns documents
        
        // When
        val stats = ragService.getDocumentStatistics(tenantId)
        
        // Then
        assertEquals(3, stats.totalDocuments)
        assertEquals(tenantId, stats.tenantId)
    }
    
    @Test
    fun `문서 통계 조회 - 전체`() {
        // Given
        every { documentRepository.count() } returns 100L
        
        // When
        val stats = ragService.getDocumentStatistics(null)
        
        // Then
        assertEquals(100, stats.totalDocuments)
        assertNull(stats.tenantId)
    }
    
    // ========================================
    // 고급 보안 테스트 (URL 검증 우회 방지)
    // ========================================
    
    @Test
    fun `출처 검증 - URL 경로 우회 시도`() {
        // Given
        val documents = listOf(
            createMockDocument(
                title = "악성",
                content = "내용",
                sourceUrl = "https://evil.com/goalmond.com/fake"  // 경로에 허용 도메인 포함
            )
        )
        
        // When
        val context = ragService.formatContextForLlm(documents)
        
        // Then
        assertEquals("관련 문서를 찾을 수 없습니다.", context)
    }
    
    @Test
    fun `출처 검증 - 서브도메인 우회 시도`() {
        // Given
        val documents = listOf(
            createMockDocument(
                title = "악성",
                content = "내용",
                sourceUrl = "https://goalmond.com.evil.com"  // 서브도메인으로 우회
            )
        )
        
        // When
        val context = ragService.formatContextForLlm(documents)
        
        // Then
        assertEquals("관련 문서를 찾을 수 없습니다.", context)
    }
    
    @Test
    fun `출처 검증 - 유사 도메인 차단`() {
        // Given
        val documents = listOf(
            createMockDocument(
                title = "악성",
                content = "내용",
                sourceUrl = "https://evil-goalmond.com"
            )
        )
        
        // When
        val context = ragService.formatContextForLlm(documents)
        
        // Then
        assertEquals("관련 문서를 찾을 수 없습니다.", context)
    }
    
    @Test
    fun `출처 검증 - JavaScript 프로토콜 차단`() {
        // Given
        val documents = listOf(
            createMockDocument(
                title = "XSS",
                content = "내용",
                sourceUrl = "javascript:alert('xss')"
            )
        )
        
        // When
        val context = ragService.formatContextForLlm(documents)
        
        // Then
        assertEquals("관련 문서를 찾을 수 없습니다.", context)
    }
    
    @Test
    fun `출처 검증 - Data URI 차단`() {
        // Given
        val documents = listOf(
            createMockDocument(
                title = "악성",
                content = "내용",
                sourceUrl = "data:text/html,<script>alert('xss')</script>"
            )
        )
        
        // When
        val context = ragService.formatContextForLlm(documents)
        
        // Then
        assertEquals("관련 문서를 찾을 수 없습니다.", context)
    }
    
    @Test
    fun `출처 검증 - 정상 서브도메인 허용`() {
        // Given
        val documents = listOf(
            createMockDocument(
                title = "정상",
                content = "내용",
                sourceUrl = "https://blog.goalmond.com/article"  // 정상 서브도메인
            )
        )
        
        // When
        val context = ragService.formatContextForLlm(documents)
        
        // Then
        assertTrue(context.contains("정상"))
        assertTrue(context.contains("blog.goalmond.com"))
    }
    
    @Test
    fun `대용량 임베딩 일괄 생성 - 배치 처리`() {
        // Given
        val documents = (1..25).map { 
            createMockDocument("문서$it", "내용$it") 
        }
        val embedding = mockk<Embedding>()
        val response = mockk<Response<Embedding>>()
        
        every { embeddingModel.embed(any<String>()) } returns response
        every { response.content() } returns embedding
        every { embedding.dimension() } returns 1536
        every { embedding.vector() } returns FloatArray(1536) { 0.1f }
        every { documentRepository.saveAll<Document>(any()) } answers { call -> 
            @Suppress("UNCHECKED_CAST")
            call.invocation.args[0] as List<Document>
        }
        
        // When
        val startTime = System.currentTimeMillis()
        val results = ragService.createAndSaveEmbeddings(documents, batchSize = 10)
        val duration = System.currentTimeMillis() - startTime
        
        // Then
        assertEquals(25, results.size)
        assertTrue(duration < 30000, "배치 처리 시간이 30초를 초과했습니다: ${duration}ms")
        
        // saveAll이 3번 호출되어야 함 (배치 크기 10, 총 25개 = 10 + 10 + 5)
        verify(exactly = 3) { documentRepository.saveAll<Document>(any()) }
    }
    
    @Test
    fun `대용량 임베딩 - 빈 목록 처리`() {
        // Given
        val emptyDocuments = emptyList<Document>()
        
        // When
        val results = ragService.createAndSaveEmbeddings(emptyDocuments)
        
        // Then
        assertTrue(results.isEmpty())
        verify(exactly = 0) { documentRepository.saveAll<Document>(any()) }
    }
    
    @Test
    fun `대용량 임베딩 - 배치 실패 시 부분 성공`() {
        // Given
        val documents = (1..15).map { 
            createMockDocument("문서$it", "내용$it") 
        }
        val embedding = mockk<Embedding>()
        val response = mockk<Response<Embedding>>()
        
        var callCount = 0
        every { embeddingModel.embed(any<String>()) } returns response
        every { response.content() } returns embedding
        every { embedding.dimension() } returns 1536
        every { embedding.vector() } returns FloatArray(1536) { 0.1f }
        every { documentRepository.saveAll<Document>(any()) } answers { call ->
            callCount++
            if (callCount == 2) {
                throw RuntimeException("배치 2 실패")
            }
            @Suppress("UNCHECKED_CAST")
            call.invocation.args[0] as List<Document>
        }
        
        // When
        val results = ragService.createAndSaveEmbeddings(documents, batchSize = 10)
        
        // Then
        // 배치 1 성공 (10개), 배치 2 실패 (5개) = 10개만 성공
        assertEquals(10, results.size)
    }
    
    // 헬퍼 메서드
    private fun createMockDocument(
        title: String,
        content: String,
        sourceUrl: String? = null
    ): Document {
        return Document(
            title = title,
            content = content,
            sourceUrl = sourceUrl
        ).apply {
            tenantId = "tenant-001"
        }
    }
}
