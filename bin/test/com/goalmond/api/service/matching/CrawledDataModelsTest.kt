package com.goalmond.api.service.matching

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * 크롤링 JSONB 파싱 모델 테스트.
 */
class CrawledDataModelsTest {

    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `facilities JSON을 FacilityInfo로 파싱할 수 있다`() {
        val json = """
            {
              "dormitory": true,
              "dining": true,
              "gym": false,
              "library": true,
              "lab": false,
              "entertainment": true
            }
        """.trimIndent()

        val parsed = objectMapper.readValue<FacilityInfo>(json)

        assertThat(parsed.dormitory).isTrue()
        assertThat(parsed.dining).isTrue()
        assertThat(parsed.gym).isFalse()
        assertThat(parsed.library).isTrue()
        assertThat(parsed.lab).isFalse()
        assertThat(parsed.entertainment).isTrue()
    }

    @Test
    fun `eslProgram JSON을 EslProgramInfo로 파싱할 수 있다`() {
        val json = """
            {
              "available": true,
              "description": "레벨별 ESL 수업을 제공합니다."
            }
        """.trimIndent()

        val parsed = objectMapper.readValue<EslProgramInfo>(json)

        assertThat(parsed.available).isTrue()
        assertThat(parsed.description).isEqualTo("레벨별 ESL 수업을 제공합니다.")
    }

    @Test
    fun `internationalSupport JSON을 InternationalSupportInfo로 파싱할 수 있다`() {
        val json = """
            {
              "available": true,
              "services": ["visa support", "housing assistance"],
              "description": "유학생 전담 코디네이터가 지원합니다."
            }
        """.trimIndent()

        val parsed = objectMapper.readValue<InternationalSupportInfo>(json)

        assertThat(parsed.available).isTrue()
        assertThat(parsed.services).containsExactly("visa support", "housing assistance")
        assertThat(parsed.description).contains("유학생")
    }

    @Test
    fun `빈 JSON은 기본값으로 파싱된다`() {
        val parsedFacility = objectMapper.readValue<FacilityInfo>("{}")
        val parsedEsl = objectMapper.readValue<EslProgramInfo>("{}")
        val parsedSupport = objectMapper.readValue<InternationalSupportInfo>("{}")

        assertThat(parsedFacility.dormitory).isFalse()
        assertThat(parsedFacility.dining).isFalse()
        assertThat(parsedEsl.available).isFalse()
        assertThat(parsedEsl.description).isNull()
        assertThat(parsedSupport.available).isFalse()
        assertThat(parsedSupport.services).isEmpty()
    }
}
