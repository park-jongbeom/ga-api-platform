package com.goalmond.api.domain.entity

import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "academic_profiles")
class AcademicProfile(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "user_id", nullable = false)
    var userId: UUID? = null,

    @Column(name = "school_name", nullable = false)
    var schoolName: String = "",

    @Column(name = "degree_type")
    var degreeType: String? = null,

    @Column(nullable = false)
    var degree: String = "",

    @Column(length = 100)
    var major: String? = null,

    @Column(name = "school_location")
    var schoolLocation: String? = null,

    @Column
    var gpa: java.math.BigDecimal? = null,

    @Column(name = "gpa_scale")
    var gpaScale: java.math.BigDecimal? = java.math.BigDecimal("4.0"),

    @Column(name = "graduation_date")
    var graduationDate: LocalDate? = null,

    @Column(length = 255)
    var institution: String? = null,

    @Column(name = "english_test_type")
    var englishTestType: String? = null,

    @Column(name = "english_score")
    var englishScore: Int? = null,

    @Column(name = "created_at")
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now()
)
