package com.goalmond.ai.config

import com.goalmond.ai.security.filter.JwtAuthenticationFilter
import com.goalmond.ai.security.filter.TenantContextFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfigurationSource

/**
 * Spring Security 설정
 * 
 * JWT 인증, 테넌트 격리, CORS, CSRF 방어 등을 설정합니다.
 * 
 * 보안 준수 항목:
 * - AuthN/AuthZ: JWT 기반 인증/인가
 * - CORS: 허용된 Origin만 접근
 * - CSRF: Stateless 환경에서 비활성화
 * - Session: Stateless 정책
 * - RBAC: 역할 기반 접근 제어
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig(
    private val corsConfigurationSource: CorsConfigurationSource,
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val tenantContextFilter: TenantContextFilter
) {
    
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            // CORS 설정
            .cors { cors ->
                cors.configurationSource(corsConfigurationSource)
            }
            // CSRF 비활성화 (JWT 사용)
            .csrf { it.disable() }
            // 세션 사용 안 함 (Stateless)
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            // 요청 권한 설정
            .authorizeHttpRequests { auth ->
                auth
                    // Swagger UI 및 API 문서는 인증 없이 접근 가능
                    .requestMatchers(
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/swagger-ui.html",
                        "/actuator/health"
                    ).permitAll()
                    // 나머지 요청은 인증 필요
                    .anyRequest().authenticated()
            }
            // JWT 인증 필터 추가
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            // 테넌트 컨텍스트 필터 추가 (JWT 이후)
            .addFilterAfter(tenantContextFilter, JwtAuthenticationFilter::class.java)
            // 보안 헤더 설정
            .headers { headers ->
                headers
                    .contentSecurityPolicy { csp ->
                        csp.policyDirectives("default-src 'self'; script-src 'self'; object-src 'none'")
                    }
                    .frameOptions { it.deny() }
                    .xssProtection { it.disable() } // 최신 브라우저는 CSP 사용
                    .httpStrictTransportSecurity { hsts ->
                        hsts.includeSubDomains(true)
                        hsts.maxAgeInSeconds(31536000)
                    }
            }
        
        return http.build()
    }
}
