package com.goalmond.api.repository

import com.goalmond.api.domain.entity.Program
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ProgramRepository : JpaRepository<Program, UUID> {
    fun findBySchoolId(schoolId: UUID): List<Program>
    fun findByType(type: String): List<Program>
    fun findBySchoolIdIn(schoolIds: List<UUID>): List<Program>
}
