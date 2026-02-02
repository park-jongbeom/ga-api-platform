package com.goalmond.api.repository

import com.goalmond.api.domain.entity.UserPreference
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface UserPreferenceRepository : JpaRepository<UserPreference, UUID> {
    fun findByUserId(userId: UUID): UserPreference?
}
