package com.goalmond.api.domain.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "user_preferences")
class UserPreference(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "user_id", nullable = false)
    var userId: UUID? = null,

    @Column
    var mbti: String? = null,

    @Column
    var tags: String? = null,

    @Column(columnDefinition = "TEXT")
    var bio: String? = null,

    @Column(name = "target_major")
    var targetMajor: String? = null,

    @Column(name = "target_location")
    var targetLocation: String? = null,

    @Column(name = "target_program")
    var targetProgram: String? = null,

    @Column(name = "budget_usd")
    var budgetUsd: Int? = null,

    @Column(name = "career_goal", columnDefinition = "TEXT")
    var careerGoal: String? = null,

    @Column(name = "preferred_track")
    var preferredTrack: String? = null,

    @Column(name = "created_at")
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now()
)
