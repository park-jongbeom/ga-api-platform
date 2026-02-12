package com.goalmond.api.domain.graphrag

/**
 * GraphRAG Ontology 검증 유틸리티.
 *
 * Entity 타입과 Relation 타입의 호환성을 검증합니다.
 *
 * 참고: [Ontology 정의](../../../../../../ai-consulting-plans/01_GRAPHRAG/ontology_definition.md)
 */
object OntologyValidator {

    /**
     * Entity 타입이 유효한지 확인합니다.
     *
     * @param entityType Entity 타입 문자열
     * @return 유효하면 true
     */
    fun isValidEntityType(entityType: String?): Boolean {
        return EntityType.isValid(entityType)
    }

    /**
     * Relation 타입이 유효한지 확인합니다.
     *
     * @param relationType Relation 타입 문자열
     * @return 유효하면 true
     */
    fun isValidRelationType(relationType: String?): Boolean {
        return RelationType.isValid(relationType)
    }

    /**
     * Relation과 Head/Tail Entity 타입의 호환성을 검증합니다.
     *
     * @param relationType Relation 타입
     * @param headEntityType Head Entity 타입
     * @param tailEntityType Tail Entity 타입
     * @return 호환되면 true
     */
    fun isCompatible(
        relationType: String?,
        headEntityType: String?,
        tailEntityType: String?
    ): Boolean {
        val relation = RelationType.fromString(relationType) ?: return false
        val head = EntityType.fromString(headEntityType) ?: return false
        val tail = EntityType.fromString(tailEntityType) ?: return false

        return when (relation) {
            RelationType.LOCATED_IN -> head == EntityType.SCHOOL && tail == EntityType.LOCATION
            RelationType.OFFERS -> head == EntityType.SCHOOL && tail == EntityType.PROGRAM
            RelationType.DEVELOPS -> head == EntityType.PROGRAM && tail == EntityType.SKILL
            RelationType.LEADS_TO -> head == EntityType.PROGRAM && tail == EntityType.JOB
            RelationType.HIRES_FROM -> head == EntityType.COMPANY && tail == EntityType.SCHOOL
            RelationType.REQUIRES -> head == EntityType.JOB && tail == EntityType.SKILL
            RelationType.PARTNERS_WITH -> head == EntityType.SCHOOL && tail == EntityType.COMPANY
        }
    }

    /**
     * Relation과 Head/Tail Entity 타입의 호환성을 검증합니다 (Enum 버전).
     *
     * @param relation Relation 타입 Enum
     * @param head Head Entity 타입 Enum
     * @param tail Tail Entity 타입 Enum
     * @return 호환되면 true
     */
    fun isCompatible(
        relation: RelationType,
        head: EntityType,
        tail: EntityType
    ): Boolean {
        return when (relation) {
            RelationType.LOCATED_IN -> head == EntityType.SCHOOL && tail == EntityType.LOCATION
            RelationType.OFFERS -> head == EntityType.SCHOOL && tail == EntityType.PROGRAM
            RelationType.DEVELOPS -> head == EntityType.PROGRAM && tail == EntityType.SKILL
            RelationType.LEADS_TO -> head == EntityType.PROGRAM && tail == EntityType.JOB
            RelationType.HIRES_FROM -> head == EntityType.COMPANY && tail == EntityType.SCHOOL
            RelationType.REQUIRES -> head == EntityType.JOB && tail == EntityType.SKILL
            RelationType.PARTNERS_WITH -> head == EntityType.SCHOOL && tail == EntityType.COMPANY
        }
    }

    /**
     * 주어진 Relation 타입에 대해 허용되는 Head Entity 타입 목록을 반환합니다.
     *
     * @param relationType Relation 타입
     * @return 허용되는 Head Entity 타입 목록
     */
    fun getAllowedHeadTypes(relationType: String?): List<EntityType> {
        val relation = RelationType.fromString(relationType) ?: return emptyList()
        return getAllowedHeadTypes(relation)
    }

    /**
     * 주어진 Relation 타입에 대해 허용되는 Head Entity 타입 목록을 반환합니다 (Enum 버전).
     */
    fun getAllowedHeadTypes(relation: RelationType): List<EntityType> {
        return when (relation) {
            RelationType.LOCATED_IN, RelationType.OFFERS, RelationType.PARTNERS_WITH -> listOf(EntityType.SCHOOL)
            RelationType.DEVELOPS, RelationType.LEADS_TO -> listOf(EntityType.PROGRAM)
            RelationType.HIRES_FROM -> listOf(EntityType.COMPANY)
            RelationType.REQUIRES -> listOf(EntityType.JOB)
        }
    }

    /**
     * 주어진 Relation 타입에 대해 허용되는 Tail Entity 타입 목록을 반환합니다.
     *
     * @param relationType Relation 타입
     * @return 허용되는 Tail Entity 타입 목록
     */
    fun getAllowedTailTypes(relationType: String?): List<EntityType> {
        val relation = RelationType.fromString(relationType) ?: return emptyList()
        return getAllowedTailTypes(relation)
    }

    /**
     * 주어진 Relation 타입에 대해 허용되는 Tail Entity 타입 목록을 반환합니다 (Enum 버전).
     */
    fun getAllowedTailTypes(relation: RelationType): List<EntityType> {
        return when (relation) {
            RelationType.LOCATED_IN -> listOf(EntityType.LOCATION)
            RelationType.OFFERS -> listOf(EntityType.PROGRAM)
            RelationType.DEVELOPS, RelationType.REQUIRES -> listOf(EntityType.SKILL)
            RelationType.LEADS_TO -> listOf(EntityType.JOB)
            RelationType.HIRES_FROM -> listOf(EntityType.SCHOOL)
            RelationType.PARTNERS_WITH -> listOf(EntityType.COMPANY)
        }
    }
}
