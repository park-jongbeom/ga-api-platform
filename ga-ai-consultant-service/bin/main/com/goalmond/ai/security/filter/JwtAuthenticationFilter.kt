package com.goalmond.ai.security.filter

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.nio.charset.StandardCharsets

/**
 * JWT 인증 필터
 * 
 * HTTP 요청의 Authorization 헤더에서 JWT 토큰을 추출하고 검증합니다.
 * 유효한 토큰이 있으면 SecurityContext에 인증 정보를 설정합니다.
 * 
 * 보안 준수 항목:
 * - AuthN: JWT 토큰 검증
 * - AuthZ: 역할(Role) 기반 권한 부여
 * - 최소 권한: 토큰에 명시된 권한만 부여
 */
@Component
class JwtAuthenticationFilter(
    @Value("\${spring.security.jwt.secret}")
    private val jwtSecret: String
) : OncePerRequestFilter() {
    
    private val logger = LoggerFactory.getLogger(javaClass)
    
    companion object {
        private const val HEADER_NAME = "Authorization"
        private const val TOKEN_PREFIX = "Bearer "
    }
    
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            // 1. Authorization 헤더에서 JWT 토큰 추출
            val token = extractTokenFromRequest(request)
            
            if (token != null) {
                // 2. JWT 토큰 검증 및 파싱
                val claims = validateAndParseToken(token)
                
                if (claims != null) {
                    // 3. SecurityContext에 인증 정보 설정
                    setAuthenticationContext(claims)
                    
                    logger.debug("JWT 인증 성공: userId=${claims.subject}")
                }
            }
        } catch (e: Exception) {
            logger.warn("JWT 인증 실패: ${e.message}")
            // 인증 실패 시 SecurityContext를 비워둠 (401 응답)
        }
        
        filterChain.doFilter(request, response)
    }
    
    /**
     * HTTP 요청에서 JWT 토큰 추출
     */
    private fun extractTokenFromRequest(request: HttpServletRequest): String? {
        val header = request.getHeader(HEADER_NAME)
        
        return if (header != null && header.startsWith(TOKEN_PREFIX)) {
            header.substring(TOKEN_PREFIX.length)
        } else {
            null
        }
    }
    
    /**
     * JWT 토큰 검증 및 파싱
     */
    private fun validateAndParseToken(token: String): Claims? {
        return try {
            val key = Keys.hmacShaKeyFor(jwtSecret.toByteArray(StandardCharsets.UTF_8))
            
            Jwts.parser()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .body
        } catch (e: Exception) {
            logger.warn("JWT 토큰 검증 실패: ${e.message}")
            null
        }
    }
    
    /**
     * SecurityContext에 인증 정보 설정
     */
    private fun setAuthenticationContext(claims: Claims) {
        val userId = claims.subject
        val roles = claims.get("roles", List::class.java) as? List<String> ?: emptyList()
        val tenantId = claims.get("tenantId", String::class.java)
        
        // GrantedAuthority 목록 생성
        val authorities = roles.map { SimpleGrantedAuthority("ROLE_$it") }
        
        // Authentication 객체 생성
        val authentication = UsernamePasswordAuthenticationToken(
            userId,
            null,
            authorities
        ).apply {
            // 추가 정보 저장 (tenantId 등)
            details = mapOf(
                "userId" to userId,
                "tenantId" to tenantId,
                "roles" to roles
            )
        }
        
        // SecurityContext에 설정
        SecurityContextHolder.getContext().authentication = authentication
    }
}
