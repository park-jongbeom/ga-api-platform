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
        whenever(client.generateContent(any())).thenReturn(
            """
            [
              {
                "school_name": "Test Community College",
                "school_type": "community_college",
                "state": "CA",
                "city": "Test City",
                "tuition": 12000,
                "program_name": "Computer Science Transfer",
                "degree": "Associate",
                "duration": "2년",
                "explanation": "테스트용 AI 추천 결과입니다.",
                "pros": ["비용 효율적", "편입 경로 제공"],
                "cons": ["2년 추가 소요"]
              }
            ]
            """.trimIndent()
        )
        return client
    }
}
