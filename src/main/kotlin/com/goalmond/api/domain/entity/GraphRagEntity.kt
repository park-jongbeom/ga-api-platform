package com.goalmond.api.domain.entity

import com.goalmond.api.domain.graphrag.EntityType
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * GraphRAG Knowledge Graph Entity.
 * 
 * 모든 Entity 타입(School, Program, Company, Job, Skill, Location)을 통합 관리하는 테이블입니다.
 */
@Entity
@Table(
    name = "entities",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["entity_type", "canonical_name"])
    ],
    indexes = [
        Index(name = "idx_entities_type_name", columnList = "entity_type,canonical_name"),
        Index(name = "idx_entities_school_id", columnList = "school_id"),
        Index(name = "idx_entities_program_id", columnList = "program_id"),
        Index(name = "idx_entities_created_at", columnList = "created_at")
    ]
)
data class GraphRagEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UUID")
    val uuid: UUID? = null,

    @Column(name = "entity_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    val entityType: EntityType,

    @Column(name = "entity_name", nullable = false, length = 255)
    val entityName: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "aliases", columnDefinition = "jsonb")
    val aliases: List<String> = emptyList(),

    @Column(name = "canonical_name", length = 255)
    val canonicalName: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    val metadata: Map<String, Any> = emptyMap(),

    @Column(name = "description", columnDefinition = "TEXT")
    val description: String? = null,

    @Column(name = "school_id", columnDefinition = "UUID")
    val schoolId: UUID? = null,

    @Column(name = "program_id", columnDefinition = "UUID")
    val programId: UUID? = null,

    @Column(name = "confidence_score", precision = 5, scale = 2)
    val confidenceScore: BigDecimal = BigDecimal.ONE,

    @Column(name = "source_urls", columnDefinition = "TEXT[]")
    val sourceUrls: Array<String> = emptyArray(),

    @Column(name = "created_at", columnDefinition = "TIMESTAMPTZ")
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", columnDefinition = "TIMESTAMPTZ")
    val updatedAt: Instant = Instant.now(),

    @Column(name = "created_by", columnDefinition = "UUID")
    val createdBy: UUID? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GraphRagEntity) return false
        return uuid != null && uuid == other.uuid
    }

    override fun hashCode(): Int = uuid?.hashCode() ?: 0
}
