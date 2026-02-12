package com.goalmond.api.domain.entity

import com.goalmond.api.domain.graphrag.RelationType
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * GraphRAG Knowledge Triple (Head-Relation-Tail).
 * 
 * Entity 간의 관계를 Triple 형태로 저장합니다.
 */
@Entity
@Table(
    name = "knowledge_triples",
    indexes = [
        Index(name = "idx_triples_head_relation", columnList = "head_entity_uuid,relation_type"),
        Index(name = "idx_triples_tail_relation", columnList = "tail_entity_uuid,relation_type"),
        Index(name = "idx_triples_relation_type", columnList = "relation_type"),
        Index(name = "idx_triples_head_tail", columnList = "head_entity_uuid,tail_entity_uuid"),
        Index(name = "idx_triples_created_at", columnList = "created_at")
    ]
)
data class KnowledgeTriple(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UUID")
    val id: UUID? = null,

    // Head Entity
    @Column(name = "head_entity_uuid", nullable = false, columnDefinition = "UUID")
    val headEntityUuid: UUID,

    @Column(name = "head_entity_type", nullable = false, length = 50)
    val headEntityType: String,

    @Column(name = "head_entity_name", nullable = false, length = 255)
    val headEntityName: String,

    // Relation
    @Column(name = "relation_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    val relationType: RelationType,

    // Tail Entity
    @Column(name = "tail_entity_uuid", nullable = false, columnDefinition = "UUID")
    val tailEntityUuid: UUID,

    @Column(name = "tail_entity_type", nullable = false, length = 50)
    val tailEntityType: String,

    @Column(name = "tail_entity_name", nullable = false, length = 255)
    val tailEntityName: String,

    // Metadata
    @Column(name = "weight", precision = 5, scale = 2)
    val weight: BigDecimal = BigDecimal.ONE,

    @Column(name = "confidence_score", precision = 5, scale = 2)
    val confidenceScore: BigDecimal = BigDecimal.ONE,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "properties", columnDefinition = "jsonb")
    val properties: Map<String, Any> = emptyMap(),

    // Source
    @Column(name = "source_url", columnDefinition = "TEXT")
    val sourceUrl: String? = null,

    @Column(name = "source_type", length = 50)
    val sourceType: String? = null,

    @Column(name = "extraction_method", length = 50)
    val extractionMethod: String? = null,

    // Audit
    @Column(name = "created_at", columnDefinition = "TIMESTAMPTZ")
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", columnDefinition = "TIMESTAMPTZ")
    val updatedAt: Instant = Instant.now(),

    @Column(name = "created_by", columnDefinition = "UUID")
    val createdBy: UUID? = null,

    @Column(name = "verified_by", columnDefinition = "UUID")
    val verifiedBy: UUID? = null,

    @Column(name = "verified_at", columnDefinition = "TIMESTAMPTZ")
    val verifiedAt: Instant? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KnowledgeTriple) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}
