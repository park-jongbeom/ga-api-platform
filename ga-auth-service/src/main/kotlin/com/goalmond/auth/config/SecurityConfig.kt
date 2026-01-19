package com.goalmond.auth.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfigurationSource

/**
 * Spring Security 설정
 * 
 * CORS를 허용하고, JWT 인증을 위한 기본 설정을 제공합니다.
 */
@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val corsConfigurationSource: CorsConfigurationSource
) {
    
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            // CORS 설정
            .cors { cors ->
                cors.configurationSource(corsConfigurationSource)
            }
            // CSRF 비활성화 (JWT 사용 시 필요)
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
                        "/swagger-ui.html"
                    ).permitAll()
                    // 인증 API는 인증 없이 접근 가능
                    .requestMatchers("/api/auth/**").permitAll()
                    // 나머지 요청은 인증 필요
                    .anyRequest().authenticated()
            }
        
        return http.build()
    }
}
