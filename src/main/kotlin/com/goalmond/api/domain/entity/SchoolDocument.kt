package com.goalmond.api.domain.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

/**
 * 학교 문서 엔티티 (RAG용).
 * V5 마이그레이션 school_documents 테이블 매핑.
 * 
 * 문서 타입:
 * - review: 한국 학생 후기, 리뷰
 * - admission_guide: 입학 가이드, 전형 팁
 * - statistics: 편입 통계, 졸업률 등
 * - pros_cons: 구체적인 장단점
 */
@Entity
@Table(name = "school_documents")
class SchoolDocument(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    
    @Column(name = "school_id", nullable = false)
    var schoolId: UUID? = null,
    
    @Column(name = "document_type", length = 50, nullable = false)
    var documentType: String = "",
    
    @Column(nullable = false)
    var title: String = "",
    
    @Column(columnDefinition = "TEXT", nullable = false)
    var content: String = "",
    
    /**
     * 768차원 임베딩 벡터.
     * pgvector는 String 형식으로 저장: "[0.1, 0.2, 0.3, ...]"
     */
    @Column(columnDefinition = "vector(768)", nullable = false)
    var embedding: String = "",
    
    /**
     * 메타데이터 (JSONB).
     * 예: {"rating": 4.5, "author": "익명", "year": 2025, "source": "공식"}
     */
    @Column(columnDefinition = "JSONB")
    var metadata: String? = null,
    
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
