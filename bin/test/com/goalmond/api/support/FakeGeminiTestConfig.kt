package com.goalmond.api.support

import com.goalmond.api.service.ai.GeminiClient
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class FakeGeminiTestConfig {
    @Bean
    @Primary
    fun geminiClient(): GeminiClient {
        val client = Mockito.mock(GeminiClient::class.java)
        val embedding = List(768) { 0.01 }
        whenever(client.embedContent(any())).thenReturn(embedding)
        // Fallback 테스트를 위한 확장된 Mock 응답 (5개 추천, 확장 필드 포함)
        whenever(client.generateContent(any())).thenReturn(
            """
            [
              {
                "school_name": "Santa Monica College",
                "school_type": "community_college",
                "state": "CA",
                "city": "Santa Monica",
                "tuition": 9000,
                "global_ranking": null,
                "ranking_field": null,
                "average_salary": null,
                "alumni_network_count": null,
                "feature_badges": ["HIGH TRANSFER RATE", "AFFORDABLE"],
                "program_name": "Computer Science AA",
                "degree": "AA",
                "duration": "2년",
                "opt_available": true,
                "recommendation_type": "safe",
                "total_score": 85,
                "score_breakdown": {"academic": 80, "english": 75, "budget": 90, "location": 85, "duration": 80, "career": 85},
                "explanation": "예산 내에서 높은 편입률을 자랑하는 커뮤니티 칼리지입니다.",
                "pros": ["높은 UC 편입률", "저렴한 학비", "LA 위치"],
                "cons": ["경쟁이 치열", "기숙사 미제공"]
              },
              {
                "school_name": "De Anza College",
                "school_type": "community_college",
                "state": "CA",
                "city": "Cupertino",
                "tuition": 9500,
                "global_ranking": null,
                "ranking_field": null,
                "average_salary": null,
                "alumni_network_count": null,
                "feature_badges": ["SILICON VALLEY", "STEM FOCUSED"],
                "program_name": "Computer Science AS",
                "degree": "AS",
                "duration": "2년",
                "opt_available": true,
                "recommendation_type": "challenge",
                "total_score": 78,
                "score_breakdown": {"academic": 75, "english": 70, "budget": 85, "location": 80, "duration": 75, "career": 80},
                "explanation": "실리콘밸리 중심에 위치한 우수한 커뮤니티 칼리지입니다.",
                "pros": ["실리콘밸리 위치", "테크 기업 연계", "STEM 프로그램"],
                "cons": ["생활비 높음", "주거 경쟁"]
              },
              {
                "school_name": "Diablo Valley College",
                "school_type": "community_college",
                "state": "CA",
                "city": "Pleasant Hill",
                "tuition": 8500,
                "global_ranking": null,
                "ranking_field": null,
                "average_salary": null,
                "alumni_network_count": null,
                "feature_badges": ["TOP UC TRANSFER", "SMALL CLASS SIZE"],
                "program_name": "General Studies AA",
                "degree": "AA",
                "duration": "2년",
                "opt_available": true,
                "recommendation_type": "safe",
                "total_score": 82,
                "score_breakdown": {"academic": 78, "english": 72, "budget": 92, "location": 75, "duration": 80, "career": 82},
                "explanation": "UC Berkeley 편입률이 높은 커뮤니티 칼리지입니다.",
                "pros": ["UC Berkeley 편입률 1위", "합리적인 학비", "소규모 수업"],
                "cons": ["대중교통 불편", "도심 외곽"]
              },
              {
                "school_name": "Orange Coast College",
                "school_type": "community_college",
                "state": "CA",
                "city": "Costa Mesa",
                "tuition": 9200,
                "global_ranking": null,
                "ranking_field": null,
                "average_salary": null,
                "alumni_network_count": null,
                "feature_badges": ["DIVERSE PROGRAMS", "OPT STEM ELIGIBLE"],
                "program_name": "Business Administration",
                "degree": "AS",
                "duration": "2년",
                "opt_available": true,
                "recommendation_type": "strategy",
                "total_score": 75,
                "score_breakdown": {"academic": 72, "english": 70, "budget": 88, "location": 78, "duration": 75, "career": 75},
                "explanation": "오렌지카운티의 대표적인 커뮤니티 칼리지입니다.",
                "pros": ["다양한 전공", "좋은 날씨", "해변 근처"],
                "cons": ["교통 혼잡", "주거비 높음"]
              },
              {
                "school_name": "Foothill College",
                "school_type": "community_college",
                "state": "CA",
                "city": "Los Altos Hills",
                "tuition": 9800,
                "global_ranking": null,
                "ranking_field": null,
                "average_salary": null,
                "alumni_network_count": null,
                "feature_badges": ["ONLINE OPTIONS", "BEAUTIFUL CAMPUS"],
                "program_name": "Computer Science Transfer",
                "degree": "AA",
                "duration": "2년",
                "opt_available": true,
                "recommendation_type": "strategy",
                "total_score": 73,
                "score_breakdown": {"academic": 70, "english": 68, "budget": 85, "location": 75, "duration": 72, "career": 75},
                "explanation": "실리콘밸리 남부에 위치한 우수한 커뮤니티 칼리지입니다.",
                "pros": ["유연한 수업 일정", "온라인 수업", "아름다운 캠퍼스"],
                "cons": ["생활비 높음", "대중교통 제한"]
              }
            ]
            """.trimIndent()
        )
        return client
    }
}
