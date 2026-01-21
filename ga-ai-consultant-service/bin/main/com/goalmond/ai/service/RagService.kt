package com.goalmond.ai.service

import com.goalmond.ai.domain.entity.Document
import com.goalmond.ai.repository.DocumentRepository
import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.model.embedding.EmbeddingModel
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * RAG (Retrieval-Augmented Generation) 서비스
 * 
 * 벡터 임베딩 기반으로 관련 문서를 검색하여 LLM에 컨텍스트를 제공합니다.
 * 
 * 참고:
 * - LangChain4j RAG: https://docs.langchain4j.dev/tutorials/rag
 * - pgvector: https://github.com/pgvector/pgvector
 */
@Service
class RagService(
    private val documentRepository: DocumentRepository,
    private val embeddingModel: EmbeddingModel
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    /**
     * 쿼리와 유사한 문서 검색 (하이브리드 검색)
     * 
     * 벡터 유사도(70%)와 키워드 매칭(30%)을 결합하여 검색 정확도를 향상시킵니다.
     * 
     * @param query 검색 쿼리
     * @param tenantId 테넌트 ID (null이면 공개 문서만 검색)
     * @param limit 반환할 최대 문서 수
     * @param documentType 문서 타입 필터 (null이면 전체)
     * @param useHybrid 하이브리드 검색 사용 여부 (기본: true)
     * @return 유사도 순으로 정렬된 문서 목록
     */
    fun searchSimilarDocuments(
        query: String,
        tenantId: String?,
        limit: Int = 5,
        documentType: String? = null,
        useHybrid: Boolean = true
    ): List<Document> {
        try {
            // 1. 쿼리를 벡터 임베딩으로 변환
            val embedding = embeddingModel.embed(query).content()
            val embeddingVector = formatEmbeddingForPostgres(embedding)
            
            logger.debug("검색 쿼리: $query, 임베딩 차원: ${embedding.dimension()}, 하이브리드: $useHybrid")
            
            // 2. 검색 실행 (하이브리드 또는 벡터 단독)
            val documents = if (useHybrid) {
                // 하이브리드 검색 (벡터 + 키워드)
                if (tenantId != null) {
                    documentRepository.findSimilarDocumentsHybrid(
                        embedding = embeddingVector,
                        keywords = query,
                        tenantId = tenantId,
                        documentType = documentType,
                        limit = limit
                    )
                } else {
                    documentRepository.findSimilarPublicDocumentsHybrid(
                        embedding = embeddingVector,
                        keywords = query,
                        limit = limit
                    )
                }
            } else {
                // 기존 벡터 단독 검색
                if (tenantId != null) {
                    documentRepository.findSimilarDocuments(embeddingVector, tenantId, limit)
                } else {
                    documentRepository.findSimilarPublicDocuments(embeddingVector, limit)
                }
            }
            
            logger.info("검색 결과: ${documents.size}개 문서 발견 (tenantId: $tenantId, type: $documentType)")
            
            return documents
        } catch (e: Exception) {
            logger.error("문서 검색 실패: ${e.message}", e)
            return emptyList()
        }
    }
    
    /**
     * 문서 임베딩 생성 및 저장
     * 
     * @param document 임베딩을 생성할 문서
     * @return 임베딩이 추가된 문서
     */
    @Transactional
    fun createAndSaveEmbedding(document: Document): Document {
        try {
            // 1. 문서 콘텐츠를 벡터 임베딩으로 변환
            val embedding = embeddingModel.embed(document.content).content()
            val embeddingVector = formatEmbeddingForPostgres(embedding)
            
            // 2. 문서에 임베딩 저장
            document.embedding = embeddingVector
            
            // 3. 데이터베이스에 저장
            val saved = documentRepository.save(document)
            
            logger.info("문서 임베딩 생성 완료: ${document.title} (${embedding.dimension()}차원)")
            
            return saved
        } catch (e: Exception) {
            logger.error("임베딩 생성 실패: ${e.message}", e)
            throw RuntimeException("문서 임베딩 생성에 실패했습니다.", e)
        }
    }
    
    /**
     * 여러 문서의 임베딩 일괄 생성
     * 
     * 성능 개선: 배치 처리 및 벌크 insert
     * 
     * @param documents 임베딩을 생성할 문서 목록
     * @param batchSize 배치 크기 (기본: 10)
     * @return 임베딩이 추가된 문서 목록
     */
    @Transactional
    fun createAndSaveEmbeddings(documents: List<Document>, batchSize: Int = 10): List<Document> {
        if (documents.isEmpty()) return emptyList()
        
        logger.info("임베딩 일괄 생성 시작: ${documents.size}개 문서")
        
        val results = mutableListOf<Document>()
        
        // 배치 단위로 처리
        documents.chunked(batchSize).forEachIndexed { batchIndex, batch ->
            try {
                logger.debug("배치 ${batchIndex + 1} 처리 중... (${batch.size}개 문서)")
                
                // 배치 임베딩 생성
                batch.forEach { document ->
                    val embedding = embeddingModel.embed(document.content).content()
                    document.embedding = formatEmbeddingForPostgres(embedding)
                }
                
                // 벌크 insert
                val savedBatch = documentRepository.saveAll(batch)
                results.addAll(savedBatch)
                
                logger.debug("배치 ${batchIndex + 1} 완료")
            } catch (e: Exception) {
                logger.error("배치 ${batchIndex + 1} 실패: ${e.message}", e)
                // 실패한 배치는 건너뛰고 계속 진행
            }
        }
        
        logger.info("임베딩 일괄 생성 완료: ${results.size}/${documents.size}개 성공")
        
        return results
    }
    
    /**
     * 검색 결과를 컨텍스트 텍스트로 변환
     * 
     * 보안 강화: 출처 URL 도메인 검증 추가
     * 
     * @param documents 검색된 문서 목록
     * @return LLM에 전달할 컨텍스트 텍스트
     */
    fun formatContextForLlm(documents: List<Document>): String {
        if (documents.isEmpty()) {
            return "관련 문서를 찾을 수 없습니다."
        }
        
        // 출처 URL 검증 (보안 강화)
        val validDocuments = documents.filter { doc ->
            doc.sourceUrl?.let { isValidSourceDomain(it) } ?: true
        }
        
        if (validDocuments.isEmpty()) {
            logger.warn("모든 문서가 출처 검증에 실패했습니다.")
            return "관련 문서를 찾을 수 없습니다."
        }
        
        val context = buildString {
            appendLine("=== 관련 지식 베이스 정보 ===")
            validDocuments.forEachIndexed { index, doc ->
                appendLine()
                appendLine("【문서 ${index + 1}: ${doc.title}】")
                appendLine(doc.content)
                
                // 출처 정보가 있으면 추가
                doc.sourceUrl?.let {
                    appendLine("출처: $it")
                }
            }
            appendLine()
            appendLine("=== 위 정보를 참고하여 답변해주세요 ===")
        }
        
        return context
    }
    
    /**
     * 출처 URL 도메인 검증
     * 
     * 허용된 도메인 목록에 있는 URL만 허용하여 보안을 강화합니다.
     * 
     * 보안 강화:
     * - contains 대신 URI 파싱으로 우회 공격 방지
     * - 정확한 호스트 검증 (서브도메인 포함)
     * 
     * @param url 검증할 URL
     * @return 유효 여부
     */
    private fun isValidSourceDomain(url: String): Boolean {
        val allowedDomains = listOf(
            "goalmond.com",
            "internal.docs",
            "docs.goalmond.com",
            "wiki.goalmond.com"
        )
        
        return try {
            // URI 파싱으로 실제 호스트 추출
            val uri = java.net.URI(url)
            val host = uri.host?.lowercase() ?: return false
            
            // 정확한 도메인 매칭 (우회 공격 방지)
            allowedDomains.any { domain ->
                host == domain || host.endsWith(".$domain")
            }
        } catch (e: Exception) {
            logger.warn("URL 파싱 실패: $url", e)
            false
        }
    }
    
    /**
     * Embedding 객체를 PostgreSQL vector 형식으로 변환
     * 
     * @param embedding LangChain4j Embedding 객체
     * @return PostgreSQL vector 문자열 (예: "[0.1, 0.2, 0.3]")
     */
    private fun formatEmbeddingForPostgres(embedding: Embedding): String {
        val vector = embedding.vector()
        return "[${vector.joinToString(",")}]"
    }
    
    /**
     * 문서 통계 정보
     */
    fun getDocumentStatistics(tenantId: String?): DocumentStatistics {
        val totalDocuments = if (tenantId != null) {
            documentRepository.findByTenantId(tenantId).size
        } else {
            documentRepository.count()
        }
        
        return DocumentStatistics(
            totalDocuments = totalDocuments.toInt(),
            tenantId = tenantId
        )
    }
}

/**
 * 문서 통계 DTO
 */
data class DocumentStatistics(
    val totalDocuments: Int,
    val tenantId: String?
)
