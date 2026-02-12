package com.goalmond.api.domain.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

/**
 * School 임베딩 엔티티 (GAM-3, Phase 0).
 * PostgreSQL pgvector extension을 활용한 벡터 저장.
 * Gemini text-embedding-004 모델 (768차원).
 */
@Entity
@Table(name = "school_embeddings")
class SchoolEmbedding(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    
    @Column(name = "school_id", nullable = false, unique = true)
    var schoolId: UUID? = null,
    
    @Column(name = "embedding_text", columnDefinition = "TEXT", nullable = false)
    var embeddingText: String = "",
    
    /**
     * 768차원 임베딩 벡터.
     * pgvector는 String 형식으로 저장: "[0.1, 0.2, 0.3, ...]"
     */
    @Column(name = "embedding", columnDefinition = "vector(768)", nullable = false)
    var embedding: String = "",
    
    @Column(name = "created_at")
    var createdAt: Instant = Instant.now(),
    
    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now()
) {
    /**
     * 임베딩 벡터를 List<Double>로 파싱
     */
    fun getEmbeddingVector(): List<Double> {
        return embedding
            .trim('[', ']')
            .split(",")
            .map { it.trim().toDouble() }
    }
    
    /**
     * List<Double>을 pgvector 형식 문자열로 변환
     */
    fun setEmbeddingVector(vector: List<Double>) {
        this.embedding = vector.joinToString(prefix = "[", postfix = "]")
    }
}
