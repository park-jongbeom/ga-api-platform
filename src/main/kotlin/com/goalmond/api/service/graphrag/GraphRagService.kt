package com.goalmond.api.service.graphrag

import com.goalmond.api.domain.entity.GraphRagEntity
import com.goalmond.api.domain.entity.KnowledgeTriple
import com.goalmond.api.domain.graphrag.EntityType
import com.goalmond.api.domain.graphrag.RelationType
import com.goalmond.api.repository.GraphRagEntityRepository
import com.goalmond.api.repository.KnowledgeTripleRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID

@Service
class GraphRagService(
    private val entityRepository: GraphRagEntityRepository,
    private val tripleRepository: KnowledgeTripleRepository
) {
    private val logger = LoggerFactory.getLogger(GraphRagService::class.java)

    @Transactional
    fun findOrCreateEntity(
        name: String,
        type: EntityType,
        aliases: List<String> = emptyList(),
        metadata: Map<String, Any> = emptyMap(),
        schoolId: UUID? = null,
        programId: UUID? = null,
        confidenceScore: Double = 1.0,
        sourceUrls: Array<String> = emptyArray()
    ): GraphRagEntity {
        val canonicalName = name.trim().lowercase()

        return entityRepository.findByEntityTypeAndCanonicalName(type, canonicalName)
            .orElseGet {
                val newEntity = GraphRagEntity(
                    entityType = type,
                    entityName = name,
                    canonicalName = canonicalName,
                    aliases = aliases,
                    metadata = metadata,
                    schoolId = schoolId,
                    programId = programId,
                    confidenceScore = BigDecimal.valueOf(confidenceScore),
                    sourceUrls = sourceUrls
                )
                val saved = entityRepository.save(newEntity)
                logger.info("Created new entity: type={}, name={}, uuid={}", type, name, saved.uuid)
                saved
            }
    }

    @Transactional
    fun createTriple(
        headEntity: GraphRagEntity,
        relation: RelationType,
        tailEntity: GraphRagEntity,
        weight: Double = 1.0,
        confidenceScore: Double = 1.0,
        properties: Map<String, Any> = emptyMap(),
        sourceUrl: String? = null,
        extractionMethod: String? = "LLM"
    ): KnowledgeTriple? {
        val existing = tripleRepository.findByHeadEntityUuidAndRelationTypeAndTailEntityUuid(
            headEntity.uuid!!,
            relation,
            tailEntity.uuid!!
        )
        
        if (existing.isNotEmpty()) {
            logger.debug("Triple already exists: {}-{}-{}", headEntity.entityName, relation, tailEntity.entityName)
            return existing.first()
        }
        
        val triple = KnowledgeTriple(
            headEntityUuid = headEntity.uuid,
            headEntityType = headEntity.entityType.name,
            headEntityName = headEntity.entityName,
            relationType = relation,
            tailEntityUuid = tailEntity.uuid,
            tailEntityType = tailEntity.entityType.name,
            tailEntityName = tailEntity.entityName,
            weight = BigDecimal.valueOf(weight),
            confidenceScore = BigDecimal.valueOf(confidenceScore),
            properties = properties,
            sourceUrl = sourceUrl,
            extractionMethod = extractionMethod
        )
        
        val saved = tripleRepository.save(triple)
        logger.info("Created triple: {}-[{}]-{}", headEntity.entityName, relation, tailEntity.entityName)
        return saved
    }

    fun getEntityById(uuid: UUID): GraphRagEntity? = entityRepository.findById(uuid).orElse(null)
    
    fun getEntitiesByType(type: EntityType): List<GraphRagEntity> = entityRepository.findByEntityType(type)
    
    fun getTriplesByEntity(entityUuid: UUID): List<KnowledgeTriple> = tripleRepository.findAllByEntityUuid(entityUuid)
}
