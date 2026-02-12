package com.goalmond.api.repository

import com.goalmond.api.domain.entity.KnowledgeTriple
import com.goalmond.api.domain.graphrag.RelationType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface KnowledgeTripleRepository : JpaRepository<KnowledgeTriple, UUID> {
    
    /**
     * Head Entity로 조회
     */
    fun findByHeadEntityUuid(headEntityUuid: UUID): List<KnowledgeTriple>
    
    /**
     * Tail Entity로 조회
     */
    fun findByTailEntityUuid(tailEntityUuid: UUID): List<KnowledgeTriple>
    
    /**
     * Relation 타입으로 조회
     */
    fun findByRelationType(relationType: RelationType): List<KnowledgeTriple>
    
    /**
     * Head Entity + Relation으로 조회
     */
    fun findByHeadEntityUuidAndRelationType(
        headEntityUuid: UUID,
        relationType: RelationType
    ): List<KnowledgeTriple>
    
    /**
     * Head + Relation + Tail 조합으로 조회 (중복 체크용)
     */
    fun findByHeadEntityUuidAndRelationTypeAndTailEntityUuid(
        headEntityUuid: UUID,
        relationType: RelationType,
        tailEntityUuid: UUID
    ): List<KnowledgeTriple>
    
    /**
     * Confidence Score 기준으로 조회
     */
    fun findByConfidenceScoreGreaterThanEqual(threshold: Double): List<KnowledgeTriple>
    
    /**
     * 특정 Entity와 연결된 모든 Triple 조회 (Head 또는 Tail)
     */
    @Query("""
        SELECT kt FROM KnowledgeTriple kt
        WHERE kt.headEntityUuid = :entityUuid
        OR kt.tailEntityUuid = :entityUuid
    """)
    fun findAllByEntityUuid(@Param("entityUuid") entityUuid: UUID): List<KnowledgeTriple>
    
    /**
     * 2-hop 관계 조회 (재귀 쿼리)
     */
    @Query("""
        SELECT kt2 FROM KnowledgeTriple kt1
        JOIN KnowledgeTriple kt2 ON kt1.tailEntityUuid = kt2.headEntityUuid
        WHERE kt1.headEntityUuid = :startEntityUuid
        AND kt1.relationType = :relation1
        AND kt2.relationType = :relation2
    """)
    fun findTwoHopRelation(
        @Param("startEntityUuid") startEntityUuid: UUID,
        @Param("relation1") relation1: RelationType,
        @Param("relation2") relation2: RelationType
    ): List<KnowledgeTriple>
}
