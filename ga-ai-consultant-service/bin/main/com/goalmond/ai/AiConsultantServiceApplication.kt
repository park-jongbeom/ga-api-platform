package com.goalmond.ai

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * AI 상담 서비스 메인 애플리케이션
 * 
 * LangChain4j 기반 AI 상담 기능을 제공하며, 민감정보 마스킹 파이프라인을 통해
 * 사용자 개인정보를 보호합니다.
 */
@SpringBootApplication(scanBasePackages = ["com.goalmond"])
class AiConsultantServiceApplication

fun main(args: Array<String>) {
    runApplication<AiConsultantServiceApplication>(*args)
}
