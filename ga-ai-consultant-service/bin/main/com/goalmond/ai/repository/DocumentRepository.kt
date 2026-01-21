package com.goalmond.ai.repository

import com.goalmond.ai.domain.entity.Document
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

/**
 * RAG 문서 Repository
 * 
 * pgvector를 활용한 벡터 유사도 검색을 지원합니다.
 */
@Repository
interface DocumentRepository : JpaRepository<Document, UUID> {
    
    /**
     * 테넌트별 문서 조회
     */
    fun findByTenantId(tenantId: String): List<Document>
    
    /**
     * 문서 타입별 조회 (테넌트 격리)
     */
    fun findByTenantIdAndDocumentType(tenantId: String, documentType: String): List<Document>
    
    /**
     * 벡터 유사도 검색
     * 
     * PostgreSQL의 pgvector 확장을 사용하여 코사인 유사도 기반 검색을 수행합니다.
     * 
     * @param embedding 쿼리 임베딩 벡터
     * @param tenantId 테넌트 ID (격리)
     * @param limit 반환할 최대 문서 수
     */
    @Query(
        value = """
            SELECT * FROM documents 
            WHERE tenant_id = :tenantId 
            ORDER BY embedding <=> CAST(:embedding AS vector) 
            LIMIT :limit
        """,
        nativeQuery = true
    )
    fun findSimilarDocuments(
        @Param("embedding") embedding: String,
        @Param("tenantId") tenantId: String,
        @Param("limit") limit: Int
    ): List<Document>
    
    /**
     * 전체 공개 문서 (테넌트 무관) 벡터 검색
     */
    @Query(
        value = """
            SELECT * FROM documents 
            WHERE tenant_id IS NULL 
            ORDER BY embedding <=> CAST(:embedding AS vector) 
            LIMIT :limit
        """,
        nativeQuery = true
    )
    fun findSimilarPublicDocuments(
        @Param("embedding") embedding: String,
        @Param("limit") limit: Int
    ): List<Document>
    
    /**
     * 하이브리드 검색 (벡터 유사도 + 키워드 BM25)
     * 
     * PostgreSQL의 pgvector와 Full-Text Search를 결합하여 검색 정확도를 향상시킵니다.
     * - 벡터 유사도: 70% 가중치
     * - 키워드 매칭(ts_rank): 30% 가중치
     * 
     * @param embedding 쿼리 임베딩 벡터
     * @param keywords 키워드 검색어
     * @param tenantId 테넌트 ID (격리)
     * @param documentType 문서 타입 필터 (null이면 전체)
     * @param limit 반환할 최대 문서 수
     * 
     * 참고: docs/reference/spring-ai-samples/08.RAG 개요.md
     */
    @Query(
        value = """
            WITH vector_search AS (
                SELECT id, 
                       (embedding <=> CAST(:embedding AS vector)) AS vector_distance,
                       content,
                       title,
                       tenant_id,
                       document_type,
                       source_url,
                       metadata,
                       created_at,
                       updated_at
                FROM documents 
                WHERE tenant_id = :tenantId 
                  AND (:documentType IS NULL OR document_type = :documentType)
            ),
            text_search AS (
                SELECT id,
                       ts_rank(
                           to_tsvector('simple', COALESCE(content, '') || ' ' || COALESCE(title, '')),
                           plainto_tsquery('simple', :keywords)
                       ) AS text_rank
                FROM documents
                WHERE tenant_id = :tenantId
                  AND (:documentType IS NULL OR document_type = :documentType)
            )
            SELECT vs.id, vs.content, vs.title, vs.tenant_id, vs.document_type, 
                   vs.source_url, vs.metadata, vs.created_at, vs.updated_at,
                   (1.0 - vs.vector_distance) * 0.7 + COALESCE(ts.text_rank, 0) * 0.3 AS hybrid_score
            FROM vector_search vs
            LEFT JOIN text_search ts ON vs.id = ts.id
            ORDER BY hybrid_score DESC
            LIMIT :limit
        """,
        nativeQuery = true
    )
    fun findSimilarDocumentsHybrid(
        @Param("embedding") embedding: String,
        @Param("keywords") keywords: String,
        @Param("tenantId") tenantId: String,
        @Param("documentType") documentType: String?,
        @Param("limit") limit: Int
    ): List<Document>
    
    /**
     * 하이브리드 검색 (공개 문서용)
     */
    @Query(
        value = """
            WITH vector_search AS (
                SELECT id, 
                       (embedding <=> CAST(:embedding AS vector)) AS vector_distance,
                       content,
                       title,
                       tenant_id,
                       document_type,
                       source_url,
                       metadata,
                       created_at,
                       updated_at
                FROM documents 
                WHERE tenant_id IS NULL
            ),
            text_search AS (
                SELECT id,
                       ts_rank(
                           to_tsvector('simple', COALESCE(content, '') || ' ' || COALESCE(title, '')),
                           plainto_tsquery('simple', :keywords)
                       ) AS text_rank
                FROM documents
                WHERE tenant_id IS NULL
            )
            SELECT vs.id, vs.content, vs.title, vs.tenant_id, vs.document_type, 
                   vs.source_url, vs.metadata, vs.created_at, vs.updated_at,
                   (1.0 - vs.vector_distance) * 0.7 + COALESCE(ts.text_rank, 0) * 0.3 AS hybrid_score
            FROM vector_search vs
            LEFT JOIN text_search ts ON vs.id = ts.id
            ORDER BY hybrid_score DESC
            LIMIT :limit
        """,
        nativeQuery = true
    )
    fun findSimilarPublicDocumentsHybrid(
        @Param("embedding") embedding: String,
        @Param("keywords") keywords: String,
        @Param("limit") limit: Int
    ): List<Document>
}
