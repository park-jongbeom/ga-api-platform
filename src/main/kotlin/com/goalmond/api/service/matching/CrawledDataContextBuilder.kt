package com.goalmond.api.service.matching

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.goalmond.api.domain.entity.School
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * schools JSONB 컬럼을 프롬프트용 컨텍스트 문장으로 변환한다.
 */
@Component
class CrawledDataContextBuilder {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val objectMapper = jacksonObjectMapper()

    fun buildCrawledDataContext(school: School): String {
        val parts = mutableListOf<String>()

        school.employmentRate?.let {
            parts.add("- 취업률: $it%")
        }

        parseFacilitiesText(school.facilities)?.let { facilitiesText ->
            parts.add("- 시설: $facilitiesText")
        }

        parseEslText(school.eslProgram)?.let { eslText ->
            parts.add("- ESL 프로그램: $eslText")
        }

        parseInternationalSupportText(school.internationalSupport)?.let { supportText ->
            parts.add("- 유학생 지원: $supportText")
        }

        school.internationalEmail?.takeIf { it.isNotBlank() }?.let {
            parts.add("- 유학생 담당 이메일: $it")
        }

        if (parts.isEmpty()) return ""
        return "\n\n[크롤링 기반 추가 정보]\n${parts.joinToString("\n")}"
    }

    private fun parseFacilitiesText(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return try {
            val facility = objectMapper.readValue<FacilityInfo>(raw)
            val list = mutableListOf<String>()
            if (facility.dormitory) list.add("기숙사")
            if (facility.dining) list.add("식당")
            if (facility.gym) list.add("체육관")
            if (facility.library) list.add("도서관")
            if (facility.lab) list.add("실습실")
            if (facility.entertainment) list.add("엔터테인먼트")
            if (list.isEmpty()) null else list.joinToString(", ")
        } catch (e: Exception) {
            logger.debug("facilities JSON 파싱 실패: {}", raw, e)
            null
        }
    }

    private fun parseEslText(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return try {
            val esl = objectMapper.readValue<EslProgramInfo>(raw)
            if (!esl.available) {
                "미제공"
            } else {
                val description = esl.description?.takeIf { it.isNotBlank() }
                if (description == null) "제공" else "제공 ($description)"
            }
        } catch (e: Exception) {
            logger.debug("esl_program JSON 파싱 실패: {}", raw, e)
            null
        }
    }

    private fun parseInternationalSupportText(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return try {
            val support = objectMapper.readValue<InternationalSupportInfo>(raw)
            if (!support.available) return null
            if (support.services.isNotEmpty()) {
                support.services.joinToString(", ")
            } else {
                support.description?.takeIf { it.isNotBlank() } ?: "지원 제공"
            }
        } catch (e: Exception) {
            logger.debug("international_support JSON 파싱 실패: {}", raw, e)
            null
        }
    }
}
