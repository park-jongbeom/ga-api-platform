package com.goalmond.api.domain.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "academic_profiles")
class AcademicProfile(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "school_name", nullable = false)
    var schoolName: String,

    @Column(name = "degree_type")
    var degreeType: String? = null,

    @Column(nullable = false)
    var degree: String,

    @Column(name = "school_location")
    var schoolLocation: String? = null,

    @Column
    var gpa: java.math.BigDecimal? = null,

    @Column(name = "english_test_type")
    var englishTestType: String? = null,

    @Column(name = "english_score")
    var englishScore: Int? = null,

    @Column(name = "created_at")
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now()
)
