package com.goalmond.api.repository

import com.goalmond.api.domain.entity.GraphRagEntity
import com.goalmond.api.domain.graphrag.EntityType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface GraphRagEntityRepository : JpaRepository<GraphRagEntity, UUID> {
    
    /**
     * Entity 타입과 canonical name으로 조회
     */
    fun findByEntityTypeAndCanonicalName(
        entityType: EntityType,
        canonicalName: String
    ): Optional<GraphRagEntity>
    
    /**
     * Entity 타입으로 조회
     */
    fun findByEntityType(entityType: EntityType): List<GraphRagEntity>
    
    /**
     * School ID로 조회
     */
    fun findBySchoolId(schoolId: UUID): List<GraphRagEntity>
    
    /**
     * Program ID로 조회
     */
    fun findByProgramId(programId: UUID): List<GraphRagEntity>
    
    /**
     * Entity 이름 검색 (aliases 포함)
     */
    @Query("""
        SELECT e FROM GraphRagEntity e
        WHERE e.entityType = :entityType
        AND (
            LOWER(e.entityName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(e.canonicalName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        )
    """)
    fun searchByTypeAndName(
        @Param("entityType") entityType: EntityType,
        @Param("searchTerm") searchTerm: String
    ): List<GraphRagEntity>
    
    /**
     * Confidence Score 기준으로 조회
     */
    fun findByConfidenceScoreGreaterThanEqual(threshold: Double): List<GraphRagEntity>
}
