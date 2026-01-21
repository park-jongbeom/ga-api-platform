package com.goalmond.ai.service

import com.goalmond.ai.security.masking.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

/**
 * MaskingService 테스트
 * 
 * 민감정보 마스킹 기능의 정확성을 검증합니다.
 */
class MaskingServiceTest {

    private lateinit var maskingService: MaskingService

    @BeforeEach
    fun setUp() {
        val strategies = listOf(
            PassportMaskingStrategy(),
            EmailMaskingStrategy(),
            PhoneMaskingStrategy(),
            GradeMaskingStrategy()
        )
        maskingService = MaskingService(strategies)
    }

    @Test
    fun `여권번호 마스킹 테스트`() {
        // Given
        val input = "My passport number is M12345678"
        
        // When
        val result = maskingService.maskSensitiveData(input)
        
        // Then
        assertTrue(result.isMasked())
        assertEquals("My passport number is [PASSPORT_001]", result.masked)
        assertEquals("M12345678", result.tokens["[PASSPORT_001]"])
    }

    @Test
    fun `이메일 마스킹 테스트`() {
        // Given
        val input = "Contact me at john.doe@example.com"
        
        // When
        val result = maskingService.maskSensitiveData(input)
        
        // Then
        assertTrue(result.isMasked())
        assertEquals("Contact me at [EMAIL_001]", result.masked)
        assertEquals("john.doe@example.com", result.tokens["[EMAIL_001]"])
    }

    @Test
    fun `전화번호 마스킹 테스트`() {
        // Given
        val input = "Call me at 010-1234-5678"
        
        // When
        val result = maskingService.maskSensitiveData(input)
        
        // Then
        assertTrue(result.isMasked())
        assertEquals("Call me at [PHONE_001]", result.masked)
        assertEquals("010-1234-5678", result.tokens["[PHONE_001]"])
    }

    @Test
    fun `GPA 마스킹 테스트`() {
        // Given
        val input = "My GPA is 3.75/4.0"
        
        // When
        val result = maskingService.maskSensitiveData(input)
        
        // Then
        assertTrue(result.isMasked())
        assertEquals("My GPA is [GPA_001]", result.masked)
        assertEquals("3.75/4.0", result.tokens["[GPA_001]"])
    }

    @Test
    fun `복합 민감정보 마스킹 테스트`() {
        // Given
        val input = """
            My name is John Doe. 
            Passport: M12345678
            Email: john@example.com
            Phone: 010-1234-5678
            GPA: 3.8/4.0
        """.trimIndent()
        
        // When
        val result = maskingService.maskSensitiveData(input)
        
        // Then
        assertTrue(result.isMasked())
        assertTrue(result.masked.contains("[PASSPORT_001]"))
        assertTrue(result.masked.contains("[EMAIL_001]"))
        assertTrue(result.masked.contains("[PHONE_001]"))
        assertTrue(result.masked.contains("[GPA_001]"))
        assertEquals(4, result.tokens.size)
    }

    @Test
    fun `복수 동일 타입 민감정보 마스킹 테스트`() {
        // Given
        val input = "Email1: test1@example.com, Email2: test2@example.com"
        
        // When
        val result = maskingService.maskSensitiveData(input)
        
        // Then
        assertTrue(result.isMasked())
        assertEquals("Email1: [EMAIL_001], Email2: [EMAIL_002]", result.masked)
        assertEquals(2, result.tokens.size)
        assertEquals("test1@example.com", result.tokens["[EMAIL_001]"])
        assertEquals("test2@example.com", result.tokens["[EMAIL_002]"])
    }

    @Test
    fun `언마스킹 테스트`() {
        // Given
        val maskedText = "My passport is [PASSPORT_001] and email is [EMAIL_001]"
        val tokens = mapOf(
            "[PASSPORT_001]" to "M12345678",
            "[EMAIL_001]" to "test@example.com"
        )
        
        // When
        val unmasked = maskingService.unmask(maskedText, tokens)
        
        // Then
        assertEquals("My passport is M12345678 and email is test@example.com", unmasked)
    }

    @Test
    fun `빈 문자열 마스킹 테스트`() {
        // Given
        val input = ""
        
        // When
        val result = maskingService.maskSensitiveData(input)
        
        // Then
        assertFalse(result.isMasked())
        assertEquals("", result.masked)
        assertTrue(result.tokens.isEmpty())
    }

    @Test
    fun `민감정보 없는 텍스트 마스킹 테스트`() {
        // Given
        val input = "This is a normal text without any sensitive information."
        
        // When
        val result = maskingService.maskSensitiveData(input)
        
        // Then
        assertFalse(result.isMasked())
        assertEquals(input, result.masked)
        assertTrue(result.tokens.isEmpty())
    }

    @Test
    fun `특수문자 포함 텍스트 마스킹 테스트`() {
        // Given
        val input = "Email: test+tag@example.com, Phone: +82-10-1234-5678"
        
        // When
        val result = maskingService.maskSensitiveData(input)
        
        // Then
        assertTrue(result.isMasked())
        assertTrue(result.masked.contains("[EMAIL_001]"))
        assertTrue(result.masked.contains("[PHONE_001]"))
    }

    @Test
    fun `마스킹 통계 조회 테스트`() {
        // When
        val stats = maskingService.getStatistics()
        
        // Then
        assertEquals(4, stats.totalStrategies)
        assertTrue(stats.strategies.any { it.first == "PassportMaskingStrategy" })
        assertTrue(stats.strategies.any { it.first == "EmailMaskingStrategy" })
        assertTrue(stats.strategies.any { it.first == "PhoneMaskingStrategy" })
        assertTrue(stats.strategies.any { it.first == "GradeMaskingStrategy" })
    }
}
