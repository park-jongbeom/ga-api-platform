package com.goalmond.api.domain.graphrag

/**
 * GraphRAG Knowledge Graph의 Relation 타입 정의.
 *
 * 참고: [Ontology 정의](../../../../../../ai-consulting-plans/01_GRAPHRAG/ontology_definition.md)
 */
enum class RelationType(val displayName: String, val description: String) {
    LOCATED_IN("LOCATED_IN", "School is located in Location"),
    OFFERS("OFFERS", "School offers Program"),
    DEVELOPS("DEVELOPS", "Program develops Skill"),
    LEADS_TO("LEADS_TO", "Program leads to Job"),
    HIRES_FROM("HIRES_FROM", "Company hires from School"),
    REQUIRES("REQUIRES", "Job requires Skill"),
    PARTNERS_WITH("PARTNERS_WITH", "School partners with Company");

    companion object {
        /**
         * 문자열로부터 RelationType을 찾습니다 (대소문자 무시).
         *
         * @param value Relation 타입 문자열
         * @return RelationType 또는 null (찾지 못한 경우)
         */
        fun fromString(value: String?): RelationType? {
            if (value == null) return null
            return values().find { 
                it.name.equals(value, ignoreCase = true) || 
                it.displayName.equals(value, ignoreCase = true) ||
                it.displayName.replace("_", "-").equals(value, ignoreCase = true)
            }
        }

        /**
         * 유효한 Relation 타입인지 확인합니다.
         */
        fun isValid(value: String?): Boolean = fromString(value) != null

        /**
         * 모든 Relation 타입 이름 목록을 반환합니다.
         */
        fun allTypeNames(): List<String> = values().map { it.displayName }
    }
}
