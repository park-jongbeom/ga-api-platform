package com.goalmond.api.domain.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

/**
 * 프로그램 문서 엔티티 (RAG용).
 * V5 마이그레이션 program_documents 테이블 매핑.
 * 
 * 문서 타입:
 * - curriculum: 커리큘럼, 수업 내용, 실습 비율
 * - career_outcome: 진로 통계, 취업률, 평균 연봉
 * - student_review: 학생 후기, 프로그램 만족도
 */
@Entity
@Table(name = "program_documents")
class ProgramDocument(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    
    @Column(name = "program_id", nullable = false)
    var programId: UUID? = null,
    
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
     * 예: {"employment_rate": 85, "avg_salary": 70000, "year": 2024}
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
