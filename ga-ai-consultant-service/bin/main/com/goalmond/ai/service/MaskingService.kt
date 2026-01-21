package com.goalmond.ai.service

import com.goalmond.ai.domain.dto.MaskedData
import com.goalmond.ai.security.masking.MaskingStrategy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 민감정보 마스킹 서비스
 * 
 * 전략 패턴을 사용하여 다양한 마스킹 규칙을 적용합니다.
 * 모든 민감정보(여권, 이메일, 전화번호, 성적 등)를 LLM 전송 전에 마스킹합니다.
 * 
 * 참고: OWASP Personal Data Protection
 * https://owasp.org/www-community/vulnerabilities/Unsafe_use_of_Reflection
 */
@Service
class MaskingService(
    strategies: List<MaskingStrategy>
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    // 우선순위에 따라 정렬된 전략 목록
    private val sortedStrategies = strategies.sortedBy { it.priority() }
    
    /**
     * 텍스트에서 민감정보를 탐지하고 마스킹합니다.
     * 
     * @param input 마스킹할 원본 텍스트
     * @return 마스킹된 데이터 (원본, 마스킹 텍스트, 토큰 매핑)
     */
    fun maskSensitiveData(input: String): MaskedData {
        if (input.isBlank()) {
            return MaskedData(input, input, emptyMap())
        }
        
        var maskedText = input
        val allTokens = mutableMapOf<String, String>()
        
        // 각 전략을 순서대로 적용
        sortedStrategies.forEach { strategy ->
            val result = strategy.mask(maskedText)
            maskedText = result.maskedText
            allTokens.putAll(result.tokens)
        }
        
        if (allTokens.isNotEmpty()) {
            logger.debug("마스킹 완료: ${allTokens.size}개 토큰 생성")
        }
        
        return MaskedData(
            original = input,
            masked = maskedText,
            tokens = allTokens
        )
    }
    
    /**
     * 마스킹된 텍스트를 원본으로 복원합니다.
     * 
     * 주의: LLM 응답에는 원본값이 아닌 마스킹 토큰이 그대로 유지되어야 합니다.
     * 이 메서드는 내부 로깅이나 관리자 뷰에서만 사용해야 합니다.
     * 
     * @param maskedText 마스킹된 텍스트
     * @param tokens 토큰 매핑
     * @return 복원된 텍스트
     */
    fun unmask(maskedText: String, tokens: Map<String, String>): String {
        var result = maskedText
        tokens.forEach { (token, original) ->
            result = result.replace(token, original)
        }
        return result
    }
    
    /**
     * 마스킹 통계 정보
     */
    fun getStatistics(): MaskingStatistics {
        return MaskingStatistics(
            totalStrategies = sortedStrategies.size,
            strategies = sortedStrategies.map { 
                Pair(it::class.simpleName ?: "Unknown", it.priority())
            }
        )
    }
}

/**
 * 마스킹 통계 DTO
 */
data class MaskingStatistics(
    val totalStrategies: Int,
    val strategies: List<Pair<String, Int>>
)
