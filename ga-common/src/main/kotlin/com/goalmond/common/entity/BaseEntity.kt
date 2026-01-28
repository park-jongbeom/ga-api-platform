package com.goalmond.common.entity

import jakarta.persistence.*
import org.hibernate.annotations.UuidGenerator
import java.time.LocalDateTime
import java.util.*

/**
 * 모든 Entity의 기본 클래스
 * UUID v4를 Primary Key로 사용하며, 생성/수정 시간을 자동 관리
 * 
 * PostgreSQL UUID 타입을 사용합니다 (BINARY(16) 아님)
 */
@MappedSuperclass
abstract class BaseEntity {
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    var id: UUID? = null

    @Column(name = "created_at", nullable = true, updatable = false)
    var createdAt: LocalDateTime? = LocalDateTime.now()

    @Column(name = "updated_at", nullable = true)
    var updatedAt: LocalDateTime? = LocalDateTime.now()

    @PreUpdate
    fun preUpdate() {
        updatedAt = LocalDateTime.now()
    }
}
