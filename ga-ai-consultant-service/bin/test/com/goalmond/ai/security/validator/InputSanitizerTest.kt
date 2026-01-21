package com.goalmond.ai.security.validator

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

/**
 * InputSanitizer 테스트
 * 
 * XSS 및 SQL Injection 공격 패턴 차단 기능을 검증합니다.
 */
class InputSanitizerTest {

    private lateinit var inputSanitizer: InputSanitizer

    @BeforeEach
    fun setUp() {
        inputSanitizer = InputSanitizer()
    }

    @Test
    fun `정상 입력값 검증 테스트`() {
        // Given
        val input = "Tell me about studying abroad in the USA"
        
        // When
        val result = inputSanitizer.validate(input)
        
        // Then
        assertTrue(result.isValid)
        assertNull(result.reason)
    }

    @Test
    fun `XSS 공격 패턴 탐지 - script 태그`() {
        // Given
        val input = "Hello<script>alert('XSS')</script>"
        
        // When
        val result = inputSanitizer.validate(input)
        
        // Then
        assertFalse(result.isValid)
        assertEquals(ThreatType.XSS, result.threatType)
        assertNotNull(result.reason)
    }

    @Test
    fun `XSS 공격 패턴 탐지 - iframe 태그`() {
        // Given
        val input = "Check this <iframe src='evil.com'></iframe>"
        
        // When
        val result = inputSanitizer.validate(input)
        
        // Then
        assertFalse(result.isValid)
        assertEquals(ThreatType.XSS, result.threatType)
    }

    @Test
    fun `XSS 공격 패턴 탐지 - javascript 프로토콜`() {
        // Given
        val input = "Click <a href='javascript:alert(1)'>here</a>"
        
        // When
        val result = inputSanitizer.validate(input)
        
        // Then
        assertFalse(result.isValid)
        assertEquals(ThreatType.XSS, result.threatType)
    }

    @Test
    fun `XSS 공격 패턴 탐지 - 이벤트 핸들러`() {
        // Given
        val input = "<img src=x onerror=alert('XSS')>"
        
        // When
        val result = inputSanitizer.validate(input)
        
        // Then
        assertFalse(result.isValid)
        assertEquals(ThreatType.XSS, result.threatType)
    }

    @Test
    fun `SQL Injection 공격 패턴 탐지 - OR 1=1`() {
        // Given
        val input = "admin' OR '1'='1"
        
        // When
        val result = inputSanitizer.validate(input)
        
        // Then
        assertFalse(result.isValid)
        assertEquals(ThreatType.SQL_INJECTION, result.threatType)
    }

    @Test
    fun `SQL Injection 공격 패턴 탐지 - UNION SELECT`() {
        // Given
        val input = "1' UNION SELECT * FROM users--"
        
        // When
        val result = inputSanitizer.validate(input)
        
        // Then
        assertFalse(result.isValid)
        assertEquals(ThreatType.SQL_INJECTION, result.threatType)
    }

    @Test
    fun `SQL Injection 공격 패턴 탐지 - DROP TABLE`() {
        // Given
        val input = "'; DROP TABLE users;--"
        
        // When
        val result = inputSanitizer.validate(input)
        
        // Then
        assertFalse(result.isValid)
        assertEquals(ThreatType.SQL_INJECTION, result.threatType)
    }

    @Test
    fun `HTML 이스케이핑 테스트`() {
        // Given
        val input = "<script>alert('test')</script>"
        
        // When
        val escaped = inputSanitizer.escapeHtml(input)
        
        // Then
        // OWASP Encoder는 슬래시(/)를 인코딩하지 않음
        assertTrue(escaped.contains("&lt;script&gt;"))
        assertTrue(escaped.contains("alert"))
        assertTrue(escaped.contains("&#39;test&#39;") || escaped.contains("&apos;test&apos;"))
        assertTrue(escaped.contains("&lt;/script&gt;"))
        assertFalse(escaped.contains("<"))
        assertFalse(escaped.contains(">"))
    }

    @Test
    fun `JavaScript 이스케이핑 테스트`() {
        // Given
        val input = "'); alert('XSS');//"
        
        // When
        val escaped = inputSanitizer.escapeJavaScript(input)
        
        // Then
        assertFalse(escaped.contains("');"))
        assertTrue(escaped.contains("\\"))
    }

    @Test
    fun `알파뉴메릭 새니타이징 테스트`() {
        // Given
        val input = "Hello<script>123가나다!?.-"
        
        // When
        val sanitized = inputSanitizer.sanitizeAlphanumeric(input)
        
        // Then
        // sanitizeAlphanumeric는 허용된 문자(알파벳, 숫자, 한글, 공백, 구두점)만 남김
        // <> 는 제거되지만, script 텍스트 자체는 알파벳이므로 남음
        assertTrue(sanitized.contains("Hello"))
        assertTrue(sanitized.contains("123"))
        assertTrue(sanitized.contains("가나다"))
        assertTrue(sanitized.contains("script"))  // <script>에서 <>만 제거, script는 남음
        assertFalse(sanitized.contains("<"))
        assertFalse(sanitized.contains(">"))
        // 결과: "Helloscript123가나다!?.-"
        assertEquals("Helloscript123가나다!?.-", sanitized)
    }

    @Test
    fun `길이 제한 테스트`() {
        // Given
        val input = "a".repeat(100)
        val maxLength = 50
        
        // When
        val limited = inputSanitizer.limitLength(input, maxLength)
        
        // Then
        assertEquals(maxLength, limited.length)
    }

    @Test
    fun `길이 제한 미만 입력 테스트`() {
        // Given
        val input = "short text"
        val maxLength = 50
        
        // When
        val limited = inputSanitizer.limitLength(input, maxLength)
        
        // Then
        assertEquals(input, limited)
        assertEquals(input.length, limited.length)
    }

    @Test
    fun `한글 포함 정상 입력 검증 테스트`() {
        // Given
        val input = "안녕하세요! 유학 상담 부탁드립니다."
        
        // When
        val result = inputSanitizer.validate(input)
        
        // Then
        assertTrue(result.isValid)
    }

    @Test
    fun `특수문자 포함 정상 입력 검증 테스트`() {
        // Given
        val input = "My GPA is 3.75/4.0 and I want to study CS."
        
        // When
        val result = inputSanitizer.validate(input)
        
        // Then
        assertTrue(result.isValid)
    }
}
