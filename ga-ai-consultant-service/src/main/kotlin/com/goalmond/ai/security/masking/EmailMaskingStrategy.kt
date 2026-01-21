package com.goalmond.ai.security.masking

import com.goalmond.ai.domain.dto.MaskingResult
import org.springframework.stereotype.Component

/**
 * 이메일 주소 마스킹 전략
 * 
 * RFC 5322 기반 이메일 주소 패턴을 탐지하여 마스킹합니다.
 */
@Component
class EmailMaskingStrategy : MaskingStrategy {
    
    // 이메일 패턴 (RFC 5322 간소화 버전)
    private val emailPattern = Regex(
        "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"
    )
    
    override fun mask(text: String): MaskingResult {
        val tokens = mutableMapOf<String, String>()
        var counter = 1
        
        val masked = emailPattern.replace(text) { matchResult ->
            val token = "[EMAIL_${counter.toString().padStart(3, '0')}]"
            tokens[token] = matchResult.value
            counter++
            token
        }
        
        return MaskingResult(masked, tokens)
    }
    
    override fun priority(): Int = 20
}
