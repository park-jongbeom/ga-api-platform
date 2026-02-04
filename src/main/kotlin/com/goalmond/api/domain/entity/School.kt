package com.goalmond.api.domain.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

/**
 * 학교 마스터 엔티티 (GAM-85).
 * V3 마이그레이션 schools 테이블 매핑.
 */
@Entity
@Table(name = "schools")
class School(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(nullable = false)
    var name: String = "",

    @Column(nullable = false, length = 50)
    var type: String = "",

    @Column(length = 100)
    var state: String? = null,

    @Column(length = 100)
    var city: String? = null,

    @Column
    var tuition: Int? = null,

    @Column(name = "living_cost")
    var livingCost: Int? = null,

    @Column
    var ranking: Int? = null,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "acceptance_rate")
    var acceptanceRate: Int? = null,

    @Column(name = "transfer_rate")
    var transferRate: Int? = null,

    @Column(name = "graduation_rate")
    var graduationRate: Int? = null,

    @Column(length = 500)
    var website: String? = null,

    @Column(name = "global_ranking", length = 50)
    var globalRanking: String? = null,

    @Column(name = "ranking_field", length = 255)
    var rankingField: String? = null,

    @Column(name = "average_salary")
    var averageSalary: Int? = null,

    @Column(name = "alumni_network_count")
    var alumniNetworkCount: Int? = null,

    @Column(name = "feature_badges", columnDefinition = "TEXT")
    var featureBadges: String? = null,

    @Column(name = "created_at")
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now()
)
