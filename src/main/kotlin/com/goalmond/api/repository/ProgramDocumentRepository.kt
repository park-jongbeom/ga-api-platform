package com.goalmond.api.repository

import com.goalmond.api.domain.entity.ProgramDocument
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * 프로그램 문서 리포지토리 (RAG용).
 */
@Repository
interface ProgramDocumentRepository : JpaRepository<ProgramDocument, UUID> {
    
    /**
     * 특정 프로그램의 모든 문서 조회.
     */
    fun findByProgramId(programId: UUID): List<ProgramDocument>
    
    /**
     * 특정 프로그램의 특정 타입 문서 조회.
     */
    fun findByProgramIdAndDocumentType(programId: UUID, documentType: String): List<ProgramDocument>
    
    /**
     * 특정 프로그램의 문서 개수 조회.
     */
    fun countByProgramId(programId: UUID): Long
    
    /**
     * 벡터 검색 쿼리 (코사인 유사도).
     * Spring AI VectorStore가 처리하므로 여기서는 기본 CRUD만 제공.
     */
    @Query(
        """
        SELECT pd FROM ProgramDocument pd 
        WHERE pd.programId = :programId 
        ORDER BY pd.createdAt DESC
        """
    )
    fun findRecentByProgramId(programId: UUID): List<ProgramDocument>
}
