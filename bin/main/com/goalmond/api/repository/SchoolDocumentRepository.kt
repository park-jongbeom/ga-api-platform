package com.goalmond.api.repository

import com.goalmond.api.domain.entity.SchoolDocument
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * 학교 문서 리포지토리 (RAG용).
 */
@Repository
interface SchoolDocumentRepository : JpaRepository<SchoolDocument, UUID> {
    
    /**
     * 특정 학교의 모든 문서 조회.
     */
    fun findBySchoolId(schoolId: UUID): List<SchoolDocument>
    
    /**
     * 특정 학교의 특정 타입 문서 조회.
     */
    fun findBySchoolIdAndDocumentType(schoolId: UUID, documentType: String): List<SchoolDocument>
    
    /**
     * 특정 학교의 문서 개수 조회.
     */
    fun countBySchoolId(schoolId: UUID): Long
    
    /**
     * 벡터 검색 쿼리 (코사인 유사도).
     * Spring AI VectorStore가 처리하므로 여기서는 기본 CRUD만 제공.
     */
    @Query(
        """
        SELECT sd FROM SchoolDocument sd 
        WHERE sd.schoolId = :schoolId 
        ORDER BY sd.createdAt DESC
        """
    )
    fun findRecentBySchoolId(schoolId: UUID): List<SchoolDocument>
}
