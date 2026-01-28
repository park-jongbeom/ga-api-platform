package com.goalmond.common.entity

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.hibernate.annotations.UuidGenerator
import java.time.LocalDateTime
import java.util.*

/**
 * 감사(Audit) 로그를 위한 베이스 Entity
 * 변경 전/후 데이터를 JSON으로 저장
 * 
 * PostgreSQL UUID 타입을 사용합니다 (BINARY(16) 아님)
 */
@MappedSuperclass
abstract class AuditEntity {
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    var id: UUID? = null

    @Column(name = "table_name", nullable = false)
    var tableName: String = ""

    @Column(name = "record_id", nullable = false)
    var recordId: UUID? = null

    @Column(nullable = false)
    var action: String = "" // CREATE, UPDATE, DELETE

    @Column(name = "actor_id")
    var actorId: UUID? = null

    @Column(name = "old_value", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    var oldValue: Map<String, Any>? = null

    @Column(name = "new_value", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    var newValue: Map<String, Any>? = null

    @Column(name = "before_data", columnDefinition = "TEXT")
    var beforeData: String? = null

    @Column(name = "after_data", columnDefinition = "TEXT")
    var afterData: String? = null

    @Column(name = "user_id", nullable = false)
    var userId: UUID? = null

    @Column(name = "ip_address", nullable = false)
    var ipAddress: String = ""

    @Column(name = "created_at", nullable = true)
    var createdAt: LocalDateTime? = LocalDateTime.now()

    @Column(name = "updated_at", nullable = true)
    var updatedAt: LocalDateTime? = LocalDateTime.now()

    @Column(name = "created_by")
    var createdBy: UUID? = null

    @Column(name = "updated_by")
    var updatedBy: UUID? = null

    @PreUpdate
    fun preUpdate() {
        updatedAt = LocalDateTime.now()
    }
}
