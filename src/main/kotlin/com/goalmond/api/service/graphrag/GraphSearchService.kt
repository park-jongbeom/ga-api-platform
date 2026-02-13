package com.goalmond.api.service.graphrag

import com.goalmond.api.domain.graphrag.EntityType
import com.goalmond.api.domain.graphrag.RelationType
import com.goalmond.api.repository.GraphRagEntityRepository
import com.goalmond.api.repository.KnowledgeTripleRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext

/**
 * Career Path 데이터 클래스
 */
data class CareerPath(
    val schoolId: UUID,
    val schoolName: String,
    val programId: UUID?,
    val programName: String?,
    val skills: List<String>,
    val job: String,
    val company: String,
    val weight: Double,
    val depth: Int,
    val path: List<String>  // 경로 시각화: ["Stanford", "CS", "ML", "Google"]
)

/**
 * Program Search Result
 */
data class ProgramSearchResult(
    val programId: UUID?,
    val programName: String,
    val schoolId: UUID?,
    val schoolName: String,
    val matchedSkills: List<String>,
    val relevanceScore: Double
)

/**
 * Graph Search Service
 * 
 * Knowledge Graph 탐색 및 경로 검색을 담당합니다.
 * - Recursive CTE 기반 경로 탐색
 * - Career Path 역추적 (Company → School)
 * - Skill 기반 Program 검색
 */
@Service
class GraphSearchService(
    private val entityRepository: GraphRagEntityRepository,
    private val tripleRepository: KnowledgeTripleRepository,
    @PersistenceContext
    private val entityManager: EntityManager
) {
    private val logger = LoggerFactory.getLogger(GraphSearchService::class.java)
    
    /**
     * Career Path 역추적: Target Company & Job → Skills → Programs → Schools
     * 
     * @param targetCompany 목표 회사명 (예: "Google", "Tesla")
     * @param targetJob 목표 직무 (예: "AI Engineer", "Software Developer")
     * @param requiredSkills 필요 스킬 (옵션)
     * @param maxDepth 최대 탐색 깊이 (기본 4)
     * @return Career Path 리스트 (가중치 기준 내림차순)
     */
    fun findCareerPaths(
        targetCompany: String,
        targetJob: String? = null,
        requiredSkills: List<String> = emptyList(),
        maxDepth: Int = 4
    ): List<CareerPath> {
        logger.info(
            "Finding career paths: company={}, job={}, skills={}",
            targetCompany, targetJob, requiredSkills
        )
        
        // 1. Company Entity 찾기
        val companyEntity = entityRepository.findByEntityTypeAndCanonicalName(
            EntityType.COMPANY,
            targetCompany.trim().lowercase()
        ).orElse(null)
        
        if (companyEntity == null) {
            logger.warn("Company not found: {}", targetCompany)
            return emptyList()
        }
        
        // 2. Recursive CTE로 경로 역추적 (Company ← Job ← Skill ← Program ← School)
        // 테스트 데이터: google-[HIRES_FROM]->aiJob-[LEADS_TO from mlSkill]->...
        // Company는 HIRES_FROM의 HEAD, 따라서 head_entity_uuid = :companyId 로 시작
        val sql = """
            WITH RECURSIVE path_search AS (
                -- Base case: Company가 고용하는 Job에서 시작 (company = HEAD of HIRES_FROM)
                SELECT
                    t.id as triple_id,
                    t.tail_entity_uuid as frontier,
                    1 as depth,
                    ARRAY[t.head_entity_uuid, t.tail_entity_uuid] as path_entities,
                    ARRAY[t.relation_type::text] as path_relations,
                    t.weight * t.confidence_score as total_weight
                FROM knowledge_triples t
                WHERE t.head_entity_uuid = :companyId
                  AND t.confidence_score >= 0.8
                  ${if (targetJob != null) "AND t.relation_type = 'HIRES_FROM'" else ""}

                UNION ALL

                -- Recursive case: frontier를 TAIL로 갖는 triple 역추적
                SELECT
                    t.id,
                    t.head_entity_uuid as frontier,
                    ps.depth + 1,
                    ps.path_entities || t.head_entity_uuid,
                    ps.path_relations || t.relation_type::text,
                    ps.total_weight * t.weight * t.confidence_score
                FROM knowledge_triples t
                JOIN path_search ps ON t.tail_entity_uuid = ps.frontier
                WHERE ps.depth < :maxDepth
                  AND t.confidence_score >= 0.8
                  AND NOT (t.head_entity_uuid = ANY(ps.path_entities))
            )
            SELECT DISTINCT
                ps.path_entities,
                ps.path_relations,
                ps.total_weight,
                ps.depth
            FROM path_search ps
            WHERE ps.depth >= 3
            ORDER BY ps.total_weight DESC
            LIMIT 20
        """.trimIndent()
        
        val query = entityManager.createNativeQuery(sql)
        query.setParameter("companyId", companyEntity.uuid)
        query.setParameter("maxDepth", maxDepth)
        
        @Suppress("UNCHECKED_CAST")
        val results = query.resultList as List<Array<Any>>
        
        // 3. 결과를 CareerPath 객체로 변환
        val careerPaths = mutableListOf<CareerPath>()
        
        for (row in results) {
            try {
                val pathEntities = (row[0] as Array<UUID>).toList()
                val pathRelations = (row[1] as Array<String>).toList()
                val totalWeight = (row[2] as Number).toDouble()
                val depth = (row[3] as Number).toInt()
                
                // Entity 정보 조회
                val entities = pathEntities.map { uuid ->
                    entityRepository.findById(uuid).orElse(null)
                }.filterNotNull()
                
                if (entities.size != pathEntities.size) {
                    logger.warn("Some entities not found in path")
                    continue
                }
                
                // School, Program, Skills 추출
                val school = entities.firstOrNull { it.entityType == EntityType.SCHOOL }
                val program = entities.firstOrNull { it.entityType == EntityType.PROGRAM }
                val skills = entities.filter { it.entityType == EntityType.SKILL }.map { it.entityName }
                val job = entities.firstOrNull { it.entityType == EntityType.JOB }
                
                if (school != null) {
                    careerPaths.add(
                        CareerPath(
                            schoolId = school.schoolId ?: school.uuid!!,
                            schoolName = school.entityName,
                            programId = program?.programId ?: program?.uuid,
                            programName = program?.entityName,
                            skills = skills,
                            job = job?.entityName ?: targetJob ?: "Unknown",
                            company = targetCompany,
                            weight = totalWeight,
                            depth = depth,
                            path = entities.map { it.entityName }
                        )
                    )
                }
            } catch (e: Exception) {
                logger.error("Error parsing career path result", e)
            }
        }
        
        logger.info("Found {} career paths for company '{}'", careerPaths.size, targetCompany)
        return careerPaths
    }
    
    /**
     * Skill 기반 Program 검색
     * 
     * @param skills 스킬 리스트
     * @param topN 상위 N개 결과 반환
     * @return Program Search Result 리스트
     */
    fun findProgramsBySkills(
        skills: List<String>,
        topN: Int = 10
    ): List<ProgramSearchResult> {
        logger.info("Finding programs by skills: {}", skills)
        
        if (skills.isEmpty()) {
            return emptyList()
        }
        
        // Skill Entity 찾기
        val skillEntities = skills.mapNotNull { skillName ->
            entityRepository.findByEntityTypeAndCanonicalName(
                EntityType.SKILL,
                skillName.trim().lowercase()
            ).orElse(null)
        }
        
        if (skillEntities.isEmpty()) {
            logger.warn("No skill entities found for: {}", skills)
            return emptyList()
        }
        
        // PROGRAM -[DEVELOPS]-> SKILL 관계 찾기
        val sql = """
            SELECT 
                e.uuid as program_uuid,
                e.entity_name as program_name,
                e.school_id,
                COUNT(DISTINCT t.tail_entity_uuid) as matched_skill_count,
                AVG(t.confidence_score * t.weight) as avg_score
            FROM entities e
            JOIN knowledge_triples t ON e.uuid = t.head_entity_uuid
            WHERE e.entity_type = 'PROGRAM'
              AND t.relation_type = 'DEVELOPS'
              AND t.tail_entity_uuid IN (:skillIds)
              AND t.confidence_score >= 0.8
            GROUP BY e.uuid, e.entity_name, e.school_id
            ORDER BY matched_skill_count DESC, avg_score DESC
            LIMIT :topN
        """.trimIndent()
        
        val query = entityManager.createNativeQuery(sql)
        query.setParameter("skillIds", skillEntities.map { it.uuid })
        query.setParameter("topN", topN)
        
        @Suppress("UNCHECKED_CAST")
        val results = query.resultList as List<Array<Any>>
        
        return results.map { row ->
            val programUuid = row[0] as UUID
            val programName = row[1] as String
            val schoolId = row[2] as? UUID
            val matchedCount = (row[3] as Number).toInt()
            val avgScore = (row[4] as Number).toDouble()
            
            // School 정보 조회
            val school = schoolId?.let { entityRepository.findById(it).orElse(null) }
            
            // 매칭된 스킬 목록 조회
            val matchedSkills = tripleRepository.findByHeadEntityUuid(programUuid)
                .filter { it.relationType == RelationType.DEVELOPS }
                .mapNotNull { triple ->
                    entityRepository.findById(triple.tailEntityUuid).orElse(null)
                }
                .filter { it.entityType == EntityType.SKILL }
                .map { it.entityName }
            
            ProgramSearchResult(
                programId = programUuid,
                programName = programName,
                schoolId = schoolId,
                schoolName = school?.entityName ?: "Unknown",
                matchedSkills = matchedSkills,
                relevanceScore = avgScore
            )
        }
    }
    
    /**
     * N-hop 이웃 검색 (1-hop, 2-hop)
     * 
     * @param startEntityId 시작 Entity UUID
     * @param hops 탐색 홉 수 (1 또는 2)
     * @return 연결된 Entity UUID 리스트
     */
    fun findNeighbors(startEntityId: UUID, hops: Int = 1): List<UUID> {
        require(hops in 1..2) { "Hops must be 1 or 2" }
        
        val sql = if (hops == 1) {
            """
                SELECT DISTINCT tail_entity_uuid
                FROM knowledge_triples
                WHERE head_entity_uuid = :entityId
                  AND confidence_score >= 0.8
            """.trimIndent()
        } else {
            // 2-hop: 1-hop 결과 + 2-hop 결과를 누적 반환 (size > 1-hop 이 되도록)
            """
                WITH hop1 AS (
                    SELECT DISTINCT tail_entity_uuid as entity_id
                    FROM knowledge_triples
                    WHERE head_entity_uuid = :entityId
                      AND confidence_score >= 0.8
                ),
                hop2 AS (
                    SELECT DISTINCT t.tail_entity_uuid as entity_id
                    FROM knowledge_triples t
                    JOIN hop1 ON t.head_entity_uuid = hop1.entity_id
                    WHERE t.confidence_score >= 0.8
                )
                SELECT entity_id FROM hop1
                UNION
                SELECT entity_id FROM hop2
            """.trimIndent()
        }
        
        val query = entityManager.createNativeQuery(sql)
        query.setParameter("entityId", startEntityId)
        
        @Suppress("UNCHECKED_CAST")
        return (query.resultList as List<UUID>)
    }
}
