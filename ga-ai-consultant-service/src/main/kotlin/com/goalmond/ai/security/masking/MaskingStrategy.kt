package com.goalmond.ai.security.masking

import com.goalmond.ai.domain.dto.MaskingResult

/**
 * 마스킹 전략 인터페이스
 * 
 * 전략 패턴을 사용하여 다양한 민감정보 마스킹 규칙을 구현합니다.
 */
interface MaskingStrategy {
    
    /**
     * 텍스트에서 민감정보를 탐지하고 마스킹합니다.
     * 
     * @param text 마스킹할 원본 텍스트
     * @return 마스킹 결과 (마스킹된 텍스트 + 토큰 매핑)
     */
    fun mask(text: String): MaskingResult
    
    /**
     * 마스킹 전략의 우선순위
     * 낮은 숫자가 먼저 실행됩니다.
     */
    fun priority(): Int = 100
}
