package com.goalmond.ai.security.validator

import org.owasp.encoder.Encode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * 입력값 검증 및 새니타이징 컴포넌트
 * 
 * XSS(Cross-Site Scripting) 및 SQLi(SQL Injection) 공격 패턴을 탐지하고 차단합니다.
 * 
 * 참고:
 * - OWASP Java Encoder: https://owasp.org/www-project-java-encoder/
 * - OWASP Input Validation: https://cheatsheetseries.owasp.org/cheatsheets/Input_Validation_Cheat_Sheet.html
 */
@Component
class InputSanitizer {
    
    private val logger = LoggerFactory.getLogger(javaClass)
    
    // XSS 공격 패턴
    private val xssPatterns = listOf(
        Regex("<script[^>]*>.*?</script>", RegexOption.IGNORE_CASE),
        Regex("<iframe[^>]*>.*?</iframe>", RegexOption.IGNORE_CASE),
        Regex("javascript:", RegexOption.IGNORE_CASE),
        Regex("on\\w+\\s*=", RegexOption.IGNORE_CASE),  // onclick, onerror 등
        Regex("<img[^>]+src[^>]*>", RegexOption.IGNORE_CASE)
    )
    
    // SQL Injection 공격 패턴
    private val sqlInjectionPatterns = listOf(
        Regex("('.*(or|and).*'=')", RegexOption.IGNORE_CASE),
        Regex("(union.*select)", RegexOption.IGNORE_CASE),
        Regex("(insert.*into)", RegexOption.IGNORE_CASE),
        Regex("(delete.*from)", RegexOption.IGNORE_CASE),
        Regex("(drop.*table)", RegexOption.IGNORE_CASE),
        Regex("(exec(ute)?\\s+(sp_|xp_))", RegexOption.IGNORE_CASE),
        Regex("(;\\s*(drop|delete|insert|update))", RegexOption.IGNORE_CASE)
    )
    
    /**
     * 입력값 검증
     * 
     * @param input 검증할 입력값
     * @return 검증 결과
     * @throws SecurityException XSS 또는 SQLi 패턴 탐지 시
     */
    fun validate(input: String): ValidationResult {
        // XSS 패턴 검증
        xssPatterns.forEach { pattern ->
            if (pattern.containsMatchIn(input)) {
                logger.warn("XSS 패턴 탐지: ${pattern.pattern}")
                return ValidationResult(
                    isValid = false,
                    reason = "XSS 공격 패턴이 탐지되었습니다.",
                    threatType = ThreatType.XSS
                )
            }
        }
        
        // SQL Injection 패턴 검증
        sqlInjectionPatterns.forEach { pattern ->
            if (pattern.containsMatchIn(input)) {
                logger.warn("SQLi 패턴 탐지: ${pattern.pattern}")
                return ValidationResult(
                    isValid = false,
                    reason = "SQL Injection 공격 패턴이 탐지되었습니다.",
                    threatType = ThreatType.SQL_INJECTION
                )
            }
        }
        
        return ValidationResult(isValid = true)
    }
    
    /**
     * HTML 컨텍스트용 이스케이핑
     * 
     * @param input 이스케이프할 문자열
     * @return HTML 안전한 문자열
     */
    fun escapeHtml(input: String): String {
        return Encode.forHtml(input)
    }
    
    /**
     * JavaScript 컨텍스트용 이스케이핑
     * 
     * @param input 이스케이프할 문자열
     * @return JavaScript 안전한 문자열
     */
    fun escapeJavaScript(input: String): String {
        return Encode.forJavaScript(input)
    }
    
    /**
     * 안전한 문자만 허용 (알파벳, 숫자, 공백, 기본 구두점)
     * 
     * @param input 필터링할 문자열
     * @return 안전한 문자만 포함된 문자열
     */
    fun sanitizeAlphanumeric(input: String): String {
        return input.replace(Regex("[^a-zA-Z0-9가-힣\\s.,!?\\-]"), "")
    }
    
    /**
     * 최대 길이 제한
     * 
     * @param input 입력 문자열
     * @param maxLength 최대 길이
     * @return 길이 제한된 문자열
     */
    fun limitLength(input: String, maxLength: Int): String {
        return if (input.length > maxLength) {
            logger.warn("입력값 길이 초과: ${input.length} > $maxLength")
            input.substring(0, maxLength)
        } else {
            input
        }
    }
}

/**
 * 검증 결과
 */
data class ValidationResult(
    val isValid: Boolean,
    val reason: String? = null,
    val threatType: ThreatType? = null
)

/**
 * 위협 유형
 */
enum class ThreatType {
    XSS,
    SQL_INJECTION,
    UNKNOWN
}
