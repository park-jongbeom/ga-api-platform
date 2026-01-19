package com.goalmond.common.entity

import jakarta.persistence.*
import org.hibernate.annotations.GenericGenerator
import java.time.LocalDateTime
import java.util.*

/**
 * 감사(Audit) 로그를 위한 베이스 Entity
 * 변경 전/후 데이터를 JSON으로 저장
 */
@MappedSuperclass
abstract class AuditEntity {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "BINARY(16)", updatable = false, nullable = false)
    var id: UUID? = null

    @Column(nullable = false)
    var tableName: String = ""

    @Column(nullable = false)
    var recordId: UUID? = null

    @Column(nullable = false)
    var action: String = "" // CREATE, UPDATE, DELETE

    @Column(columnDefinition = "TEXT")
    var beforeData: String? = null

    @Column(columnDefinition = "TEXT")
    var afterData: String? = null

    @Column(nullable = false)
    var userId: UUID? = null

    @Column(nullable = false)
    var ipAddress: String = ""

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
}
