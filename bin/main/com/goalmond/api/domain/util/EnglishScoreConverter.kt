package com.goalmond.api.domain.util

/**
 * 영어 점수 환산 유틸리티 (GAM-3, Phase 4).
 * 
 * IELTS, Duolingo 점수를 TOEFL 점수로 환산합니다.
 * 
 * 참조: https://www.ets.org/toefl/score-users/scores-admissions/compare.html
 */
object EnglishScoreConverter {
    
    /**
     * IELTS → TOEFL 환산 테이블.
     * 
     * IELTS는 0.5 단위, TOEFL은 0-120 범위입니다.
     */
    private val ieltsToToeflMap = mapOf(
        9.0 to 118,
        8.5 to 115,
        8.0 to 110,
        7.5 to 102,
        7.0 to 94,
        6.5 to 79,
        6.0 to 60,
        5.5 to 46,
        5.0 to 35,
        4.5 to 32,
        4.0 to 0
    )
    
    /**
     * Duolingo → TOEFL 환산 테이블.
     * 
     * Duolingo는 10-160 범위입니다.
     */
    private val duolingoToToeflMap = mapOf(
        160 to 120,
        150 to 117,
        140 to 113,
        130 to 107,
        120 to 100,
        110 to 92,
        100 to 83,
        95 to 78,
        90 to 72,
        85 to 66,
        80 to 60,
        75 to 54,
        70 to 48,
        65 to 42,
        60 to 36,
        55 to 30,
        50 to 24,
        10 to 0
    )
    
    /**
     * IELTS 점수를 TOEFL로 환산.
     * 
     * @param ielts IELTS 점수 (0.0 ~ 9.0)
     * @return TOEFL 점수 (0 ~ 120), null이면 환산 불가
     */
    fun ieltsToToefl(ielts: Double): Int {
        // 정확히 매칭되는 점수가 있으면 반환
        ieltsToToeflMap[ielts]?.let { return it }
        
        // 범위 내에서 선형 보간
        val lowerBound = ieltsToToeflMap.keys.filter { it <= ielts }.maxOrNull() ?: 4.0
        val upperBound = ieltsToToeflMap.keys.filter { it >= ielts }.minOrNull() ?: 9.0
        
        if (lowerBound == upperBound) {
            return ieltsToToeflMap[lowerBound] ?: 0
        }
        
        val lowerToefl = ieltsToToeflMap[lowerBound] ?: 0
        val upperToefl = ieltsToToeflMap[upperBound] ?: 120
        
        // 선형 보간
        val ratio = (ielts - lowerBound) / (upperBound - lowerBound)
        return (lowerToefl + ratio * (upperToefl - lowerToefl)).toInt()
    }
    
    /**
     * Duolingo 점수를 TOEFL로 환산.
     * 
     * @param duolingo Duolingo 점수 (10 ~ 160)
     * @return TOEFL 점수 (0 ~ 120), null이면 환산 불가
     */
    fun duolingoToToefl(duolingo: Int): Int {
        // 정확히 매칭되는 점수가 있으면 반환
        duolingoToToeflMap[duolingo]?.let { return it }
        
        // 범위 내에서 선형 보간
        val lowerBound = duolingoToToeflMap.keys.filter { it <= duolingo }.maxOrNull() ?: 10
        val upperBound = duolingoToToeflMap.keys.filter { it >= duolingo }.minOrNull() ?: 160
        
        if (lowerBound == upperBound) {
            return duolingoToToeflMap[lowerBound] ?: 0
        }
        
        val lowerToefl = duolingoToToeflMap[lowerBound] ?: 0
        val upperToefl = duolingoToToeflMap[upperBound] ?: 120
        
        // 선형 보간
        val ratio = (duolingo - lowerBound).toDouble() / (upperBound - lowerBound)
        return (lowerToefl + ratio * (upperToefl - lowerToefl)).toInt()
    }
    
    /**
     * 영어 점수를 TOEFL로 통일.
     * 
     * @param testType 영어 시험 유형 (TOEFL, IELTS, Duolingo)
     * @param score 점수
     * @return TOEFL 환산 점수
     */
    fun convertToToefl(testType: String?, score: Int?): Int {
        if (score == null || score <= 0) return 0
        
        return when (testType?.uppercase()) {
            "TOEFL" -> score
            "IELTS" -> ieltsToToefl(score.toDouble())
            "DUOLINGO" -> duolingoToToefl(score)
            else -> 0 // 알 수 없는 유형은 0으로 처리
        }
    }
}
