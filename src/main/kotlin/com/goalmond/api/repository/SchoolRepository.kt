package com.goalmond.api.repository

import com.goalmond.api.domain.entity.School
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SchoolRepository : JpaRepository<School, UUID> {
    fun findByType(type: String): List<School>
    fun findByState(state: String): List<School>
    fun findByNameStartingWith(prefix: String): List<School>
}
