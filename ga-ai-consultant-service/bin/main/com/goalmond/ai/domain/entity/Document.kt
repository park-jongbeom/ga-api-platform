package com.goalmond.ai.domain.entity

import com.goalmond.common.entity.BaseEntity
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

/**
 * RAG 문서 Entity
 * 
 * 유학 관련 지식 베이스 문서를 나타냅니다.
 * pgvector를 사용한 벡터 임베딩을 저장하여 의미 기반 검색을 지원합니다.
 */
@Entity
@Table(
    name = "documents",
    indexes = [
        Index(name = "idx_tenant", columnList = "tenant_id"),
        Index(name = "idx_created_at", columnList = "created_at")
    ]
)
class Document(
    
    @Column(nullable = false, length = 255)
    var title: String,
    
    @Column(columnDefinition = "TEXT", nullable = false)
    var content: String,
    
    @Column(name = "embedding", columnDefinition = "vector(1536)")
    var embedding: String? = null,
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    var metadata: Map<String, Any>? = null,
    
    @Column(name = "tenant_id", length = 50)
    var tenantId: String? = null,
    
    @Column(name = "source_url", length = 500)
    var sourceUrl: String? = null,
    
    @Column(name = "document_type", length = 50)
    var documentType: String? = null
    
) : BaseEntity()
