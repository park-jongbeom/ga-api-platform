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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

/**
 * 동시성 및 멀티스레드 환경 테스트
 * 
 * 테스트 범위:
 * - 여러 테넌트 동시 검색
 * - 테넌트 격리 유지 검증
 * - 스레드 안전성
 * - 데드락 방지
 * 
 * @author AI Consultant Team
 * @since 2026-01-21
 */
class ConcurrencyTest {
    
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
    fun `동시성 테스트 - 여러 테넌트 동시 검색`() {
        // Given
        val tenants = listOf("tenant-001", "tenant-002", "tenant-003")
        val threadCount = tenants.size
        val latch = CountDownLatch(threadCount)
        val successCount = AtomicInteger(0)
        val results = mutableMapOf<String, List<Document>>()
        
        val embedding = mockk<Embedding>()
        val response = mockk<Response<Embedding>>()
        
        every { embeddingModel.embed(any<String>()) } returns response
        every { response.content() } returns embedding
        every { embedding.dimension() } returns 1536
        every { embedding.vector() } returns FloatArray(1536) { 0.1f }
        
        tenants.forEach { tenant ->
            val documents = listOf(
                createMockDocument(tenant, "테넌트 $tenant 문서", "내용")
            )
            every { 
                documentRepository.findSimilarDocumentsHybrid(any(), any(), tenant, null, 5) 
            } returns documents
        }
        
        // When
        val threads = tenants.map { tenant ->
            thread {
                try {
                    val docs = ragService.searchSimilarDocuments(
                        query = "테스트 쿼리",
                        tenantId = tenant,
                        limit = 5
                    )
                    synchronized(results) {
                        results[tenant] = docs
                    }
                    successCount.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }
        
        // 모든 스레드 완료 대기 (최대 10초)
        assertTrue(latch.await(10, TimeUnit.SECONDS), "스레드 실행 타임아웃")
        
        // Then
        assertEquals(threadCount, successCount.get(), "모든 스레드가 성공해야 함")
        
        // 각 테넌트의 결과가 올바른지 검증
        tenants.forEach { tenant ->
            assertNotNull(results[tenant])
            assertEquals(1, results[tenant]!!.size)
            assertTrue(results[tenant]!![0].title.contains(tenant))
        }
        
        // 각 테넌트별로 정확히 1번씩 호출되었는지 검증
        tenants.forEach { tenant ->
            verify(exactly = 1) { 
                documentRepository.findSimilarDocumentsHybrid(any(), any(), tenant, null, 5) 
            }
        }
    }
    
    @Test
    fun `동시성 테스트 - 테넌트 격리 검증`() {
        // Given
        val tenant1 = "tenant-001"
        val tenant2 = "tenant-002"
        val threadCount = 10
        val latch = CountDownLatch(threadCount)
        
        val embedding = mockk<Embedding>()
        val response = mockk<Response<Embedding>>()
        
        every { embeddingModel.embed(any<String>()) } returns response
        every { response.content() } returns embedding
        every { embedding.dimension() } returns 1536
        every { embedding.vector() } returns FloatArray(1536) { 0.1f }
        
        val docs1 = listOf(createMockDocument(tenant1, "문서1", "내용1"))
        val docs2 = listOf(createMockDocument(tenant2, "문서2", "내용2"))
        
        every { documentRepository.findSimilarDocumentsHybrid(any(), any(), tenant1, null, 5) } returns docs1
        every { documentRepository.findSimilarDocumentsHybrid(any(), any(), tenant2, null, 5) } returns docs2
        
        // When
        val results = mutableListOf<List<Document>>()
        val threads = (1..threadCount).map { i ->
            thread {
                try {
                    val tenant = if (i % 2 == 0) tenant1 else tenant2
                    val docs = ragService.searchSimilarDocuments(
                        query = "테스트",
                        tenantId = tenant,
                        limit = 5
                    )
                    synchronized(results) {
                        results.add(docs)
                    }
                } finally {
                    latch.countDown()
                }
            }
        }
        
        assertTrue(latch.await(10, TimeUnit.SECONDS))
        
        // Then
        assertEquals(threadCount, results.size)
        
        // tenant1 문서와 tenant2 문서가 섞이지 않았는지 검증
        results.forEach { docs ->
            if (docs.isNotEmpty()) {
                val firstTenant = docs[0].tenantId
                assertTrue(docs.all { it.tenantId == firstTenant }, "테넌트 격리가 유지되어야 함")
            }
        }
    }
    
    @Test
    fun `스레드 안전성 - 문서 통계 동시 조회`() {
        // Given
        val threadCount = 20
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)
        val results = mutableListOf<DocumentStatistics>()
        
        every { documentRepository.count() } returns 100L
        
        // When
        repeat(threadCount) {
            executor.submit {
                try {
                    val stats = ragService.getDocumentStatistics(null)
                    synchronized(results) {
                        results.add(stats)
                    }
                } finally {
                    latch.countDown()
                }
            }
        }
        
        assertTrue(latch.await(10, TimeUnit.SECONDS))
        executor.shutdown()
        
        // Then
        assertEquals(threadCount, results.size)
        assertTrue(results.all { it.totalDocuments == 100 })
    }
    
    @Test
    fun `동시성 테스트 - 임베딩 생성 경쟁 상태`() {
        // Given
        val documents = (1..5).map { createMockDocument("tenant-001", "문서$it", "내용$it") }
        val threadCount = 3
        val latch = CountDownLatch(threadCount)
        val allResults = mutableListOf<List<Document>>()
        
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
        
        // When: 여러 스레드가 동시에 같은 문서 리스트를 임베딩
        val threads = (1..threadCount).map {
            thread {
                try {
                    val results = ragService.createAndSaveEmbeddings(documents.toList())
                    synchronized(allResults) {
                        allResults.add(results)
                    }
                } finally {
                    latch.countDown()
                }
            }
        }
        
        assertTrue(latch.await(10, TimeUnit.SECONDS))
        
        // Then
        assertEquals(threadCount, allResults.size)
        assertTrue(allResults.all { it.size == 5 })
    }
    
    // 헬퍼 메서드
    private fun createMockDocument(
        tenantId: String,
        title: String,
        content: String,
        sourceUrl: String? = null
    ): Document {
        return Document(
            title = title,
            content = content,
            sourceUrl = sourceUrl
        ).apply {
            this.tenantId = tenantId
        }
    }
}
