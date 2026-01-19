package com.goalmond.user.entity

import com.goalmond.common.entity.BaseEntity
import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "users")
class User : BaseEntity() {
    @Column(nullable = false, unique = true)
    var email: String = ""

    @Column(nullable = false)
    var passwordHash: String = ""

    @Column(nullable = false)
    var name: String = ""

    @Column(nullable = false)
    var role: String = "USER" // USER, ADMIN

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

    @Column(nullable = false)
    var degree: String = "" // BACHELOR, MASTER, PHD

    @Column(nullable = false)
    var major: String = ""

    @Column
    var gpa: Double? = null

    @Column
    var institution: String = ""
}

@Entity
@Table(name = "financial_profiles")
class FinancialProfile : BaseEntity() {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User? = null

    @Column(nullable = false)
    var budgetRange: String = ""

    @Column
    var fundingSource: String = ""
}

@Entity
@Table(name = "user_preferences")
class UserPreference : BaseEntity() {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User? = null

    @Column(nullable = false)
    var preferredMajor: String = ""

    @Column
    var careerTrack: String = ""
}
