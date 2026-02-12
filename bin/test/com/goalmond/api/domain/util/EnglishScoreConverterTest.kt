package com.goalmond.api.domain.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * EnglishScoreConverter 단위 테스트 (GAM-3, Phase 4).
 * 
 * 테스트 목표:
 * - IELTS/Duolingo 환산 검증
 */
class EnglishScoreConverterTest {
    
    @Test
    fun `IELTS 7_0을 TOEFL 94로 환산`() {
        val toefl = EnglishScoreConverter.ieltsToToefl(7.0)
        assertThat(toefl).isEqualTo(94)
    }
    
    @Test
    fun `IELTS 6_0을 TOEFL 60으로 환산`() {
        val toefl = EnglishScoreConverter.ieltsToToefl(6.0)
        assertThat(toefl).isEqualTo(60)
    }
    
    @Test
    fun `IELTS 8_5를 TOEFL 115로 환산`() {
        val toefl = EnglishScoreConverter.ieltsToToefl(8.5)
        assertThat(toefl).isEqualTo(115)
    }
    
    @Test
    fun `IELTS 6_5를 TOEFL 79로 환산`() {
        val toefl = EnglishScoreConverter.ieltsToToefl(6.5)
        assertThat(toefl).isEqualTo(79)
    }
    
    @Test
    fun `Duolingo 120을 TOEFL 100으로 환산`() {
        val toefl = EnglishScoreConverter.duolingoToToefl(120)
        assertThat(toefl).isEqualTo(100)
    }
    
    @Test
    fun `Duolingo 100을 TOEFL 83으로 환산`() {
        val toefl = EnglishScoreConverter.duolingoToToefl(100)
        assertThat(toefl).isEqualTo(83)
    }
    
    @Test
    fun `Duolingo 90을 TOEFL 72로 환산`() {
        val toefl = EnglishScoreConverter.duolingoToToefl(90)
        assertThat(toefl).isEqualTo(72)
    }
    
    @Test
    fun `convertToToefl이 TOEFL 점수를 그대로 반환`() {
        val result = EnglishScoreConverter.convertToToefl("TOEFL", 95)
        assertThat(result).isEqualTo(95)
    }
    
    @Test
    fun `convertToToefl이 IELTS를 TOEFL로 환산`() {
        // IELTS는 소수점 점수이지만 Int로 전달 시 정수로 처리
        val result = EnglishScoreConverter.convertToToefl("IELTS", 7) // 7.0 = 94
        assertThat(result).isEqualTo(94)
    }
    
    @Test
    fun `convertToToefl이 Duolingo를 TOEFL로 환산`() {
        val result = EnglishScoreConverter.convertToToefl("Duolingo", 110)
        assertThat(result).isEqualTo(92)
    }
    
    @Test
    fun `알 수 없는 시험 유형은 0 반환`() {
        val result = EnglishScoreConverter.convertToToefl("UNKNOWN", 100)
        assertThat(result).isEqualTo(0)
    }
    
    @Test
    fun `null 점수는 0 반환`() {
        val result = EnglishScoreConverter.convertToToefl("TOEFL", null)
        assertThat(result).isEqualTo(0)
    }
}
