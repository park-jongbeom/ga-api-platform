package com.goalmond.api.domain.graphrag

/**
 * GraphRAG Knowledge Graph의 Entity 타입 정의.
 *
 * 참고: [Ontology 정의](../../../../../../ai-consulting-plans/01_GRAPHRAG/ontology_definition.md)
 */
enum class EntityType(val displayName: String) {
    SCHOOL("School"),
    PROGRAM("Program"),
    COMPANY("Company"),
    JOB("Job"),
    SKILL("Skill"),
    LOCATION("Location");

    companion object {
        /**
         * 문자열로부터 EntityType을 찾습니다 (대소문자 무시).
         *
         * @param value Entity 타입 문자열
         * @return EntityType 또는 null (찾지 못한 경우)
         */
        fun fromString(value: String?): EntityType? {
            if (value == null) return null
            return values().find { 
                it.name.equals(value, ignoreCase = true) || 
                it.displayName.equals(value, ignoreCase = true) 
            }
        }

        /**
         * 유효한 Entity 타입인지 확인합니다.
         */
        fun isValid(value: String?): Boolean = fromString(value) != null

        /**
         * 모든 Entity 타입 이름 목록을 반환합니다.
         */
        fun allTypeNames(): List<String> = values().map { it.displayName }
    }
}
