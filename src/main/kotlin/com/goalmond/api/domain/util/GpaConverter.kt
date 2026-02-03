package com.goalmond.api.domain.util

import java.math.BigDecimal

/**
 * GPA 정규화 유틸리티 (GAM-3, Phase 5).
 * 
 * 한국 내신, 백분율 등을 4.0 스케일로 정규화합니다.
 */
object GpaConverter {
    
    /**
     * GPA를 4.0 스케일로 정규화.
     * 
     * @param gpa 원본 GPA
     * @param scale 원본 스케일 (예: 4.0, 4.3, 4.5, 100.0)
     * @return 4.0 스케일로 정규화된 GPA
     */
    fun normalize(gpa: BigDecimal?, scale: BigDecimal?): Double {
        if (gpa == null || scale == null || scale == BigDecimal.ZERO) {
            return 0.0
        }
        
        val gpaDouble = gpa.toDouble()
        val scaleDouble = scale.toDouble()
        
        return when {
            // 이미 4.0 스케일
            scaleDouble == 4.0 -> gpaDouble
            
            // 4.3 스케일 (한국 일부 대학)
            scaleDouble == 4.3 -> (gpaDouble / 4.3) * 4.0
            
            // 4.5 스케일 (한국 일부 대학)
            scaleDouble == 4.5 -> (gpaDouble / 4.5) * 4.0
            
            // 5.0 스케일
            scaleDouble == 5.0 -> (gpaDouble / 5.0) * 4.0
            
            // 백분율 (100점 만점)
            scaleDouble == 100.0 -> (gpaDouble / 100.0) * 4.0
            
            // 9등급 내신 (1등급 = 4.0, 9등급 = 0.0)
            scaleDouble == 9.0 -> convertKoreanGrade(gpaDouble)
            
            // 기타: 비율로 환산
            else -> (gpaDouble / scaleDouble) * 4.0
        }.coerceIn(0.0, 4.0)
    }
    
    /**
     * 한국 내신 등급 → 4.0 스케일 변환.
     * 
     * 1등급 = 4.0
     * 2등급 = 3.7
     * 3등급 = 3.3
     * 4등급 = 3.0
     * 5등급 = 2.7
     * 6등급 = 2.3
     * 7등급 = 2.0
     * 8등급 = 1.5
     * 9등급 = 0.0
     */
    private fun convertKoreanGrade(grade: Double): Double {
        return when {
            grade <= 1.0 -> 4.0
            grade <= 2.0 -> 3.7
            grade <= 3.0 -> 3.3
            grade <= 4.0 -> 3.0
            grade <= 5.0 -> 2.7
            grade <= 6.0 -> 2.3
            grade <= 7.0 -> 2.0
            grade <= 8.0 -> 1.5
            else -> 0.0
        }
    }
}
