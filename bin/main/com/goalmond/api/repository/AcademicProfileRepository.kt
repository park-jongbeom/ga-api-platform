package com.goalmond.api.repository

import com.goalmond.api.domain.entity.AcademicProfile
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AcademicProfileRepository : JpaRepository<AcademicProfile, UUID> {
    fun findByUserId(userId: UUID): AcademicProfile?
}
