package com.goalmond.ai.security.masking

import com.goalmond.ai.domain.dto.MaskingResult
import org.springframework.stereotype.Component

/**
 * 전화번호 마스킹 전략
 * 
 * 한국, 미국 등 주요 국가의 전화번호 패턴을 탐지하여 마스킹합니다.
 * 
 * 패턴:
 * - 한국: 010-1234-5678, 02-1234-5678, +82-10-1234-5678
 * - 미국: (123) 456-7890, 123-456-7890, +1-123-456-7890
 */
@Component
class PhoneMaskingStrategy : MaskingStrategy {
    
    // 전화번호 패턴 (한국 + 미국 + 국제 형식)
    private val phonePatterns = listOf(
        // 한국 전화번호
        Regex("\\+?82-?\\d{1,2}-?\\d{3,4}-?\\d{4}"),
        Regex("0\\d{1,2}-?\\d{3,4}-?\\d{4}"),
        // 미국 전화번호
        Regex("\\+?1-?\\(?\\d{3}\\)?-?\\d{3}-?\\d{4}"),
        // 일반 국제 형식
        Regex("\\+\\d{1,3}\\s?\\d{1,4}\\s?\\d{1,4}\\s?\\d{1,9}")
    )
    
    override fun mask(text: String): MaskingResult {
        val tokens = mutableMapOf<String, String>()
        var counter = 1
        var masked = text
        
        phonePatterns.forEach { pattern ->
            masked = pattern.replace(masked) { matchResult ->
                val token = "[PHONE_${counter.toString().padStart(3, '0')}]"
                tokens[token] = matchResult.value
                counter++
                token
            }
        }
        
        return MaskingResult(masked, tokens)
    }
    
    override fun priority(): Int = 30
}
