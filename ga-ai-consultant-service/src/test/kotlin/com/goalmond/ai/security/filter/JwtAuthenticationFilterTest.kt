package com.goalmond.ai.security.filter

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.core.context.SecurityContextHolder
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * JwtAuthenticationFilter 테스트
 * 
 * JWT 토큰 검증 및 인증 처리를 검증합니다.
 */
class JwtAuthenticationFilterTest {

    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter
    private lateinit var request: HttpServletRequest
    private lateinit var response: HttpServletResponse
    private lateinit var filterChain: FilterChain
    
    private val jwtSecret = "test-secret-key-for-junit-tests-only-256-bits-long-key"

    @BeforeEach
    fun setUp() {
        jwtAuthenticationFilter = JwtAuthenticationFilter(jwtSecret)
        request = mock()
        response = mock()
        filterChain = mock()
        
        // SecurityContext 초기화
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `유효한 JWT 토큰 인증 성공 테스트`() {
        // Given
        val userId = "user-123"
        val tenantId = "tenant-a"
        val roles = listOf("USER")
        
        val token = generateValidToken(userId, tenantId, roles)
        whenever(request.getHeader("Authorization")).thenReturn("Bearer $token")
        
        // When
        jwtAuthenticationFilter.doFilter(request, response, filterChain)
        
        // Then
        val authentication = SecurityContextHolder.getContext().authentication
        assertNotNull(authentication)
        assertEquals(userId, authentication.principal)
        assertTrue(authentication.isAuthenticated)
        
        val details = authentication.details as Map<*, *>
        assertEquals(userId, details["userId"])
        assertEquals(tenantId, details["tenantId"])
        
        verify(filterChain).doFilter(request, response)
    }

    @Test
    fun `Authorization 헤더 없음 테스트`() {
        // Given
        whenever(request.getHeader("Authorization")).thenReturn(null)
        
        // When
        jwtAuthenticationFilter.doFilter(request, response, filterChain)
        
        // Then
        val authentication = SecurityContextHolder.getContext().authentication
        assertNull(authentication)
        
        verify(filterChain).doFilter(request, response)
    }

    @Test
    fun `잘못된 토큰 형식 테스트`() {
        // Given
        whenever(request.getHeader("Authorization")).thenReturn("InvalidToken")
        
        // When
        jwtAuthenticationFilter.doFilter(request, response, filterChain)
        
        // Then
        val authentication = SecurityContextHolder.getContext().authentication
        assertNull(authentication)
        
        verify(filterChain).doFilter(request, response)
    }

    @Test
    fun `만료된 토큰 테스트`() {
        // Given
        val expiredToken = generateExpiredToken("user-123", "tenant-a")
        whenever(request.getHeader("Authorization")).thenReturn("Bearer $expiredToken")
        
        // When
        jwtAuthenticationFilter.doFilter(request, response, filterChain)
        
        // Then
        val authentication = SecurityContextHolder.getContext().authentication
        assertNull(authentication)
        
        verify(filterChain).doFilter(request, response)
    }

    @Test
    fun `변조된 토큰 테스트`() {
        // Given
        val validToken = generateValidToken("user-123", "tenant-a", listOf("USER"))
        val tamperedToken = validToken + "tampered"
        whenever(request.getHeader("Authorization")).thenReturn("Bearer $tamperedToken")
        
        // When
        jwtAuthenticationFilter.doFilter(request, response, filterChain)
        
        // Then
        val authentication = SecurityContextHolder.getContext().authentication
        assertNull(authentication)
        
        verify(filterChain).doFilter(request, response)
    }

    /**
     * 유효한 JWT 토큰 생성
     */
    private fun generateValidToken(userId: String, tenantId: String, roles: List<String>): String {
        val key = Keys.hmacShaKeyFor(jwtSecret.toByteArray(StandardCharsets.UTF_8))
        
        return Jwts.builder()
            .setSubject(userId)
            .claim("tenantId", tenantId)
            .claim("roles", roles)
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + 3600000)) // 1시간
            .signWith(key)
            .compact()
    }

    /**
     * 만료된 JWT 토큰 생성
     */
    private fun generateExpiredToken(userId: String, tenantId: String): String {
        val key = Keys.hmacShaKeyFor(jwtSecret.toByteArray(StandardCharsets.UTF_8))
        
        return Jwts.builder()
            .setSubject(userId)
            .claim("tenantId", tenantId)
            .setIssuedAt(Date(System.currentTimeMillis() - 7200000)) // 2시간 전
            .setExpiration(Date(System.currentTimeMillis() - 3600000)) // 1시간 전 만료
            .signWith(key)
            .compact()
    }
}
