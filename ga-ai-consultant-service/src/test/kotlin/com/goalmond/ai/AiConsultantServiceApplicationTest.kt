package com.goalmond.ai

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

/**
 * AI 상담 서비스 애플리케이션 컨텍스트 로딩 테스트
 * 
 * Spring Boot 애플리케이션이 정상적으로 시작되는지 검증합니다.
 */
@SpringBootTest
@ActiveProfiles("test")
class AiConsultantServiceApplicationTest {

    @Test
    fun `컨텍스트 로딩 테스트`() {
        // Spring Context가 정상적으로 로드되면 테스트 성공
    }
}
