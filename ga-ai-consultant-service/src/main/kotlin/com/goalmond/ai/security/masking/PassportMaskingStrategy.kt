package com.goalmond.ai.security.masking

import com.goalmond.ai.domain.dto.MaskingResult
import org.springframework.stereotype.Component

/**
 * 여권번호 마스킹 전략
 * 
 * 한국, 미국, 영국 등 주요 국가의 여권번호 패턴을 탐지하여 마스킹합니다.
 * 
 * 패턴:
 * - 한국: M12345678 (1자 알파벳 + 8자리 숫자)
 * - 미국: 123456789 (9자리 숫자)
 * - 영국: 123456789 (9자리 숫자)
 */
@Component
class PassportMaskingStrategy : MaskingStrategy {
    
    // 여권번호 패턴 (1자 알파벳 + 8자리 숫자 또는 9자리 숫자)
    private val passportPattern = Regex(
        "\\b([A-Z]\\d{8}|\\d{9})\\b",
        RegexOption.IGNORE_CASE
    )
    
    override fun mask(text: String): MaskingResult {
        val tokens = mutableMapOf<String, String>()
        var counter = 1
        
        val masked = passportPattern.replace(text) { matchResult ->
            val token = "[PASSPORT_${counter.toString().padStart(3, '0')}]"
            tokens[token] = matchResult.value
            counter++
            token
        }
        
        return MaskingResult(masked, tokens)
    }
    
    override fun priority(): Int = 10
}
