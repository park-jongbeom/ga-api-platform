package com.goalmond.api.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val env: Environment,
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private val jwtAuthenticationFilter: JwtAuthenticationFilter?
) {

    private val isDefaultProfile: Boolean
        get() = env.activeProfiles.isEmpty() || env.activeProfiles.contains("default")

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        return if (isDefaultProfile) {
            // default 프로파일: Mock API만 사용, 인증 없이 전체 허용
            http
                .csrf { it.disable() }
                .authorizeHttpRequests { it.anyRequest().permitAll() }
                .build()
        } else {
            // local, lightsail 프로파일: JWT 인증 적용
            http
                .csrf { it.disable() }
                .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
                .authorizeHttpRequests { auth ->
                    auth
                        .requestMatchers("/api/v1/auth/signup", "/api/v1/auth/login").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/api/v1/**").authenticated()
                        .anyRequest().permitAll()
                }
                .addFilterBefore(
                    jwtAuthenticationFilter!!,
                    UsernamePasswordAuthenticationFilter::class.java
                )
                .build()
        }
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}
