package com.goalmond.ai.domain.dto

/**
 * 마스킹된 데이터 DTO
 * 
 * 원본 텍스트와 마스킹된 텍스트, 그리고 토큰 매핑 정보를 포함합니다.
 */
data class MaskedData(
    /** 원본 텍스트 (민감정보 포함) */
    val original: String,
    
    /** 마스킹된 텍스트 (LLM 전송용) */
    val masked: String,
    
    /** 마스킹 토큰과 원본값의 매핑 (예: [PASSPORT_001] -> M12345678) */
    val tokens: Map<String, String>
) {
    /**
     * 마스킹된 텍스트에 토큰을 원본값으로 복원
     * 
     * @param maskedText LLM 응답 등 마스킹 토큰이 포함된 텍스트
     * @return 토큰이 원본값으로 복원된 텍스트
     */
    fun unmask(maskedText: String): String {
        var result = maskedText
        tokens.forEach { (token, original) ->
            result = result.replace(token, original)
        }
        return result
    }
    
    /**
     * 마스킹 여부 확인
     */
    fun isMasked(): Boolean = tokens.isNotEmpty()
}

/**
 * 마스킹 결과 (전략별)
 */
data class MaskingResult(
    /** 마스킹 처리된 텍스트 */
    val maskedText: String,
    
    /** 이번 마스킹에서 생성된 토큰 매핑 */
    val tokens: Map<String, String>
)
