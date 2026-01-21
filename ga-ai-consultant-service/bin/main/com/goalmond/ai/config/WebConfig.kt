package com.goalmond.ai.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * Web MVC 설정
 * 
 * CORS, 인터셉터 등을 설정합니다.
 * 
 * 보안 준수 항목:
 * - CORS: 허용된 Origin만 접근
 * - Preflight: OPTIONS 요청 처리
 * - Credentials: 쿠키/인증 헤더 허용
 */
@Configuration
class WebConfig : WebMvcConfigurer {
    
    @Value("\${cors.allowed-origins}")
    private lateinit var allowedOrigins: String
    
    @Value("\${cors.allowed-methods}")
    private lateinit var allowedMethods: String
    
    @Value("\${cors.allowed-headers}")
    private lateinit var allowedHeaders: String
    
    @Value("\${cors.allow-credentials}")
    private var allowCredentials: Boolean = true
    
    @Value("\${cors.max-age}")
    private var maxAge: Long = 3600
    
    /**
     * Spring Security에서 사용할 CORS 설정 소스
     */
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        
        // 허용된 Origin (환경변수로 관리)
        configuration.allowedOrigins = allowedOrigins.split(",").map { it.trim() }
        
        // 허용된 HTTP 메서드
        configuration.allowedMethods = allowedMethods.split(",").map { it.trim() }
        
        // 허용된 헤더
        if (allowedHeaders == "*") {
            configuration.addAllowedHeader("*")
        } else {
            configuration.allowedHeaders = allowedHeaders.split(",").map { it.trim() }
        }
        
        // 인증 정보 허용 (쿠키, Authorization 헤더 등)
        configuration.allowCredentials = allowCredentials
        
        // Preflight 요청 캐시 시간 (초)
        configuration.maxAge = maxAge
        
        // 노출할 응답 헤더
        configuration.exposedHeaders = listOf(
            "Authorization",
            "Content-Type",
            "X-Requested-With"
        )
        
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
