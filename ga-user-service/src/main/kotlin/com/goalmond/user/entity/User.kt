package com.goalmond.user.entity

import com.goalmond.common.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "users")
class User : BaseEntity() {
    @Column(nullable = false, unique = true)
    var email: String = ""

    @Column(name = "password_hash")
    var passwordHash: String? = null

    @Column(name = "full_name", nullable = false)
    var fullName: String = ""

    @Column(nullable = true)
    var role: String = "STUDENT" // USER, ADMIN, STUDENT

    @Column(name = "is_active")
    var isActive: Boolean? = true

    @Column(name = "email_verified")
    var emailVerified: Boolean? = false

    @Column(name = "email_verification_token")
    var emailVerificationToken: String? = null

    @Column(name = "password_reset_token")
    var passwordResetToken: String? = null

    @Column(name = "password_reset_expires_at")
    var passwordResetExpiresAt: LocalDateTime? = null

    @Column(name = "created_by")
    var createdBy: UUID? = null

    @Column(name = "updated_by")
    var updatedBy: UUID? = null

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true)
    var academicProfiles: MutableList<AcademicProfile> = mutableListOf()

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true)
    var financialProfiles: MutableList<FinancialProfile> = mutableListOf()

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true)
    var preferences: MutableList<UserPreference> = mutableListOf()
}

@Entity
@Table(name = "academic_profiles")
class AcademicProfile : BaseEntity() {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User? = null

    @Column(name = "school_name", nullable = false)
    var schoolName: String = ""

    @Column(name = "degree_type")
    var degreeType: String? = null

    @Column(nullable = false)
    var degree: String = "" // BACHELOR, MASTER, PHD

    @Column
    var major: String? = null

    @Column(columnDefinition = "NUMERIC(4,2)")
    var gpa: java.math.BigDecimal? = null

    @Column(name = "gpa_scale", columnDefinition = "NUMERIC(4,2)")
    var gpaScale: java.math.BigDecimal? = java.math.BigDecimal("4.0")

    @Column(name = "graduation_date")
    var graduationDate: java.time.LocalDate? = null

    @Column
    var institution: String? = null

    @Column(name = "created_by")
    var createdBy: UUID? = null

    @Column(name = "updated_by")
    var updatedBy: UUID? = null
}

@Entity
@Table(name = "financial_profiles")
class FinancialProfile : BaseEntity() {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User? = null

    @Column(name = "budget_range", nullable = false)
    var budgetRange: String = ""

    @Column(name = "total_budget_usd")
    var totalBudgetUsd: Int? = null

    @Column(name = "tuition_limit_usd")
    var tuitionLimitUsd: Int? = null

    @Column(name = "funding_source", columnDefinition = "TEXT")
    var fundingSource: String? = null

    @Column(name = "created_by")
    var createdBy: UUID? = null

    @Column(name = "updated_by")
    var updatedBy: UUID? = null
}

@Entity
@Table(name = "user_preferences")
class UserPreference : BaseEntity() {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User? = null

    @Column(name = "target_major")
    var targetMajor: String? = null

    @Column(name = "target_location")
    var targetLocation: String? = null

    @Column(name = "career_goal", columnDefinition = "TEXT")
    var careerGoal: String? = null

    @Column(name = "preferred_track")
    var preferredTrack: String? = null

    @Column(name = "created_by")
    var createdBy: UUID? = null

    @Column(name = "updated_by")
    var updatedBy: UUID? = null
}
