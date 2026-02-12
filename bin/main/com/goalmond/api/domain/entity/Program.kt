package com.goalmond.api.domain.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

/**
 * 프로그램 마스터 엔티티 (GAM-86).
 * V3 마이그레이션 programs 테이블 매핑.
 */
@Entity
@Table(name = "programs")
class Program(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "school_id", nullable = false)
    var schoolId: UUID? = null,

    @Column(nullable = false)
    var name: String = "",

    @Column(nullable = false, length = 50)
    var type: String = "",

    @Column(length = 50)
    var degree: String? = null,

    @Column(length = 100)
    var duration: String? = null,

    @Column
    var tuition: Int? = null,

    @Column(name = "opt_available", nullable = false)
    var optAvailable: Boolean = true,

    @Column(name = "created_at")
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now()
)
