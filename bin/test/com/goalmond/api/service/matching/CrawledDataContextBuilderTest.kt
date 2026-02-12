package com.goalmond.api.service.matching

import com.goalmond.api.domain.entity.School
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class CrawledDataContextBuilderTest {

    private val builder = CrawledDataContextBuilder()

    @Test
    fun `크롤링 데이터가 있으면 프롬프트 컨텍스트에 포함된다`() {
        val school = School(
            name = "Test College",
            type = "community_college",
            state = "CA",
            city = "Irvine",
            employmentRate = BigDecimal("87.5"),
            facilities = """{"dormitory": true, "dining": true, "gym": true}""",
            eslProgram = """{"available": true, "description": "레벨별 ESL"}""",
            internationalSupport = """{"available": true, "services": ["visa support", "housing assistance"]}""",
            internationalEmail = "intl@test.edu"
        )

        val context = builder.buildCrawledDataContext(school)

        assertThat(context).contains("취업률: 87.5%")
        assertThat(context).contains("기숙사")
        assertThat(context).contains("식당")
        assertThat(context).contains("체육관")
        assertThat(context).contains("ESL 프로그램: 제공")
        assertThat(context).contains("visa support")
        assertThat(context).contains("intl@test.edu")
    }

    @Test
    fun `크롤링 데이터가 없으면 빈 문자열을 반환한다`() {
        val school = School(
            name = "Test College",
            type = "community_college",
            state = "CA",
            city = "Irvine"
        )

        val context = builder.buildCrawledDataContext(school)

        assertThat(context).isEmpty()
    }

    @Test
    fun `잘못된 JSON은 무시하고 나머지 정보는 유지한다`() {
        val school = School(
            name = "Test College",
            type = "community_college",
            state = "CA",
            city = "Irvine",
            employmentRate = BigDecimal("80.0"),
            facilities = "{invalid-json}",
            eslProgram = """{"available": true}"""
        )

        val context = builder.buildCrawledDataContext(school)

        assertThat(context).contains("취업률: 80.0%")
        assertThat(context).contains("ESL 프로그램: 제공")
        assertThat(context).doesNotContain("시설:")
    }
}
