package com.goalmond.api.domain.graphrag

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("OntologyValidator 테스트")
class OntologyValidatorTest {

    @Test
    @DisplayName("유효한 Entity 타입 검증")
    fun `유효한 Entity 타입 검증`() {
        assertThat(OntologyValidator.isValidEntityType("School")).isTrue()
        assertThat(OntologyValidator.isValidEntityType("Program")).isTrue()
        assertThat(OntologyValidator.isValidEntityType("Company")).isTrue()
        assertThat(OntologyValidator.isValidEntityType("Job")).isTrue()
        assertThat(OntologyValidator.isValidEntityType("Skill")).isTrue()
        assertThat(OntologyValidator.isValidEntityType("Location")).isTrue()
        
        assertThat(OntologyValidator.isValidEntityType("Invalid")).isFalse()
        assertThat(OntologyValidator.isValidEntityType(null)).isFalse()
        assertThat(OntologyValidator.isValidEntityType("")).isFalse()
    }

    @Test
    @DisplayName("대소문자 무시 Entity 타입 검증")
    fun `대소문자 무시 Entity 타입 검증`() {
        assertThat(OntologyValidator.isValidEntityType("school")).isTrue()
        assertThat(OntologyValidator.isValidEntityType("SCHOOL")).isTrue()
        assertThat(OntologyValidator.isValidEntityType("School")).isTrue()
    }

    @Test
    @DisplayName("유효한 Relation 타입 검증")
    fun `유효한 Relation 타입 검증`() {
        assertThat(OntologyValidator.isValidRelationType("LOCATED_IN")).isTrue()
        assertThat(OntologyValidator.isValidRelationType("OFFERS")).isTrue()
        assertThat(OntologyValidator.isValidRelationType("DEVELOPS")).isTrue()
        assertThat(OntologyValidator.isValidRelationType("LEADS_TO")).isTrue()
        assertThat(OntologyValidator.isValidRelationType("HIRES_FROM")).isTrue()
        assertThat(OntologyValidator.isValidRelationType("REQUIRES")).isTrue()
        assertThat(OntologyValidator.isValidRelationType("PARTNERS_WITH")).isTrue()
        
        assertThat(OntologyValidator.isValidRelationType("INVALID")).isFalse()
        assertThat(OntologyValidator.isValidRelationType(null)).isFalse()
    }

    @Test
    @DisplayName("Relation과 Entity 타입 호환성 검증 - OFFERS")
    fun `OFFERS 관계 호환성 검증`() {
        assertThat(OntologyValidator.isCompatible("OFFERS", "School", "Program")).isTrue()
        assertThat(OntologyValidator.isCompatible("OFFERS", "Program", "School")).isFalse()
        assertThat(OntologyValidator.isCompatible("OFFERS", "School", "School")).isFalse()
        
        assertThat(
            OntologyValidator.isCompatible(
                RelationType.OFFERS,
                EntityType.SCHOOL,
                EntityType.PROGRAM
            )
        ).isTrue()
    }

    @Test
    @DisplayName("Relation과 Entity 타입 호환성 검증 - LOCATED_IN")
    fun `LOCATED_IN 관계 호환성 검증`() {
        assertThat(OntologyValidator.isCompatible("LOCATED_IN", "School", "Location")).isTrue()
        assertThat(OntologyValidator.isCompatible("LOCATED_IN", "Location", "School")).isFalse()
    }

    @Test
    @DisplayName("Relation과 Entity 타입 호환성 검증 - DEVELOPS")
    fun `DEVELOPS 관계 호환성 검증`() {
        assertThat(OntologyValidator.isCompatible("DEVELOPS", "Program", "Skill")).isTrue()
        assertThat(OntologyValidator.isCompatible("DEVELOPS", "School", "Skill")).isFalse()
    }

    @Test
    @DisplayName("Relation과 Entity 타입 호환성 검증 - LEADS_TO")
    fun `LEADS_TO 관계 호환성 검증`() {
        assertThat(OntologyValidator.isCompatible("LEADS_TO", "Program", "Job")).isTrue()
        assertThat(OntologyValidator.isCompatible("LEADS_TO", "School", "Job")).isFalse()
    }

    @Test
    @DisplayName("Relation과 Entity 타입 호환성 검증 - HIRES_FROM")
    fun `HIRES_FROM 관계 호환성 검증`() {
        assertThat(OntologyValidator.isCompatible("HIRES_FROM", "Company", "School")).isTrue()
        assertThat(OntologyValidator.isCompatible("HIRES_FROM", "School", "Company")).isFalse()
    }

    @Test
    @DisplayName("Relation과 Entity 타입 호환성 검증 - REQUIRES")
    fun `REQUIRES 관계 호환성 검증`() {
        assertThat(OntologyValidator.isCompatible("REQUIRES", "Job", "Skill")).isTrue()
        assertThat(OntologyValidator.isCompatible("REQUIRES", "Program", "Skill")).isFalse()
    }

    @Test
    @DisplayName("Relation과 Entity 타입 호환성 검증 - PARTNERS_WITH")
    fun `PARTNERS_WITH 관계 호환성 검증`() {
        assertThat(OntologyValidator.isCompatible("PARTNERS_WITH", "School", "Company")).isTrue()
        assertThat(OntologyValidator.isCompatible("PARTNERS_WITH", "Company", "School")).isFalse()
    }

    @Test
    @DisplayName("허용되는 Head Entity 타입 조회")
    fun `허용되는 Head Entity 타입 조회`() {
        val headTypes = OntologyValidator.getAllowedHeadTypes("OFFERS")
        assertThat(headTypes).containsExactly(EntityType.SCHOOL)
        
        val headTypesEnum = OntologyValidator.getAllowedHeadTypes(RelationType.DEVELOPS)
        assertThat(headTypesEnum).containsExactly(EntityType.PROGRAM)
    }

    @Test
    @DisplayName("허용되는 Tail Entity 타입 조회")
    fun `허용되는 Tail Entity 타입 조회`() {
        val tailTypes = OntologyValidator.getAllowedTailTypes("OFFERS")
        assertThat(tailTypes).containsExactly(EntityType.PROGRAM)
        
        val tailTypesHires = OntologyValidator.getAllowedTailTypes(RelationType.HIRES_FROM)
        assertThat(tailTypesHires).containsExactly(EntityType.SCHOOL)
        
        val tailTypesPartners = OntologyValidator.getAllowedTailTypes(RelationType.PARTNERS_WITH)
        assertThat(tailTypesPartners).containsExactly(EntityType.COMPANY)
    }

    @Test
    @DisplayName("EntityType fromString 테스트")
    fun `EntityType fromString 테스트`() {
        assertThat(EntityType.fromString("School")).isEqualTo(EntityType.SCHOOL)
        assertThat(EntityType.fromString("school")).isEqualTo(EntityType.SCHOOL)
        assertThat(EntityType.fromString("SCHOOL")).isEqualTo(EntityType.SCHOOL)
        assertThat(EntityType.fromString("Invalid")).isNull()
        assertThat(EntityType.fromString(null)).isNull()
    }

    @Test
    @DisplayName("RelationType fromString 테스트")
    fun `RelationType fromString 테스트`() {
        assertThat(RelationType.fromString("OFFERS")).isEqualTo(RelationType.OFFERS)
        assertThat(RelationType.fromString("offers")).isEqualTo(RelationType.OFFERS)
        assertThat(RelationType.fromString("LEADS-TO")).isEqualTo(RelationType.LEADS_TO)
        assertThat(RelationType.fromString("Invalid")).isNull()
        assertThat(RelationType.fromString(null)).isNull()
    }
}
