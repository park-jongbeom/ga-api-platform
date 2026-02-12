package com.goalmond.api.repository

import com.goalmond.api.domain.entity.SchoolEmbedding
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * School 임베딩 Repository (GAM-3, Phase 0).
 * pgvector 코사인 유사도 검색 지원.
 */
@Repository
interface SchoolEmbeddingRepository : JpaRepository<SchoolEmbedding, UUID> {
    
    /**
     * school_id로 임베딩 조회
     */
    fun findBySchoolId(schoolId: UUID): SchoolEmbedding?
    fun countBySchoolId(schoolId: UUID): Long
    
    /**
     * 코사인 유사도 기반 벡터 검색 (Top K).
     * pgvector의 <=> 연산자: 코사인 거리 (0 = 동일, 2 = 정반대)
     * 유사도 = 1 - 코사인 거리 (1 = 동일, -1 = 정반대)
     * 
     * @param queryEmbedding 쿼리 벡터 (pgvector 형식: "[0.1, 0.2, ...]")
     * @param limit 반환할 상위 K개
     * @return school_id와 유사도 점수 목록 (유사도 내림차순)
     */
    @Query(value = """
        SELECT se.school_id AS schoolId, 
               1 - (se.embedding <=> CAST(:queryEmbedding AS vector)) AS similarity
        FROM school_embeddings se
        ORDER BY se.embedding <=> CAST(:queryEmbedding AS vector)
        LIMIT :limit
    """, nativeQuery = true)
    fun findTopByCosineSimilarity(
        @Param("queryEmbedding") queryEmbedding: String,
        @Param("limit") limit: Int
    ): List<VectorSearchResult>
    
    /**
     * 전체 임베딩 개수 조회
     */
    override fun count(): Long
    
    /**
     * Native Query로 벡터 삽입.
     * Hibernate가 pgvector 타입을 직접 처리하지 못하므로 Native Query 사용.
     * 
     * @param schoolId School ID
     * @param embeddingText 원본 텍스트
     * @param embeddingVector 벡터 문자열 "[0.1, 0.2, ...]"
     */
    @Modifying
    @Query(value = """
        INSERT INTO school_embeddings (id, school_id, embedding_text, embedding, created_at, updated_at)
        VALUES (gen_random_uuid(), :schoolId, :embeddingText, CAST(:embeddingVector AS vector), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        ON CONFLICT (school_id) 
        DO UPDATE SET 
            embedding_text = EXCLUDED.embedding_text,
            embedding = EXCLUDED.embedding,
            updated_at = CURRENT_TIMESTAMP
    """, nativeQuery = true)
    fun insertOrUpdateEmbedding(
        @Param("schoolId") schoolId: UUID,
        @Param("embeddingText") embeddingText: String,
        @Param("embeddingVector") embeddingVector: String
    )
}

/**
 * 벡터 검색 결과 DTO.
 * Native Query 결과 매핑용 인터페이스.
 */
interface VectorSearchResult {
    fun getSchoolId(): UUID
    fun getSimilarity(): Double
}
