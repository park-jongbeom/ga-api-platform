package com.goalmond.ai.security.masking

import com.goalmond.ai.domain.dto.MaskingResult
import org.springframework.stereotype.Component

/**
 * 성적 정보 마스킹 전략
 * 
 * 성적표 관련 민감 정보를 탐지하여 마스킹합니다.
 * 
 * 패턴:
 * - GPA: 3.75/4.0, 3.75/4.5
 * - 점수: 95점, 95/100
 * - 등급: A+, B+, C 등
 */
@Component
class GradeMaskingStrategy : MaskingStrategy {
    
    // GPA 패턴 (예: 3.75/4.0, 3.75/4.5)
    private val gpaPattern = Regex(
        "\\b([0-4]\\.[0-9]{1,2})\\s?/\\s?([4-5]\\.[0-9])\\b"
    )
    
    // 점수 패턴 (예: 95점, 95/100)
    private val scorePattern = Regex(
        "\\b(\\d{1,3})\\s?(?:점|/\\s?100)\\b"
    )
    
    // 등급 패턴 (예: A+, B-, C 등)
    private val gradePattern = Regex(
        "\\b([A-F][+-]?|Pass|Fail)\\s?등급\\b",
        RegexOption.IGNORE_CASE
    )
    
    override fun mask(text: String): MaskingResult {
        val tokens = mutableMapOf<String, String>()
        var counter = 1
        var masked = text
        
        // GPA 마스킹
        masked = gpaPattern.replace(masked) { matchResult ->
            val token = "[GPA_${counter.toString().padStart(3, '0')}]"
            tokens[token] = matchResult.value
            counter++
            token
        }
        
        // 점수 마스킹
        masked = scorePattern.replace(masked) { matchResult ->
            val token = "[SCORE_${counter.toString().padStart(3, '0')}]"
            tokens[token] = matchResult.value
            counter++
            token
        }
        
        // 등급 마스킹
        masked = gradePattern.replace(masked) { matchResult ->
            val token = "[GRADE_${counter.toString().padStart(3, '0')}]"
            tokens[token] = matchResult.value
            counter++
            token
        }
        
        return MaskingResult(masked, tokens)
    }
    
    override fun priority(): Int = 40
}
