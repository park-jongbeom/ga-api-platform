package com.goalmond.api.service.matching

import com.goalmond.api.domain.entity.AcademicProfile
import com.goalmond.api.domain.entity.Program
import com.goalmond.api.domain.entity.School
import com.goalmond.api.domain.entity.UserPreference
import com.goalmond.api.domain.util.EnglishScoreConverter
import org.springframework.stereotype.Service

/**
 * 리스크 패널티 서비스 (GAM-3, Phase 6).
 * 
 * 위험 요소에 대해 패널티를 적용합니다.
 * 
 * 4가지 패널티:
 * 1. 입학 경쟁률 과도 (< 30%) → -15점
 * 2. 영어 점수 임계 (여유 < 5점) → -10점
 * 3. 예산 임계 (여유 < $3,000) → -10점
 * 4. 체류 의사 불명확 (OPT 의사 없음) → -5점
 */
@Service
class RiskPenaltyService {
    
    /**
     * 리스크 패널티 계산.
     * 
     * @return 패널티 (0 ~ -40)
     */
    fun applyPenalty(
        profile: AcademicProfile,
        preference: UserPreference,
        program: Program,
        school: School
    ): Double {
        var penalty = 0.0
        
        // 패널티 1: 입학 경쟁률 과도
        if (isHighCompetition(school)) {
            penalty -= 15.0
        }
        
        // 패널티 2: 영어 점수 임계
        if (isEnglishScoreMarginal(profile, program)) {
            penalty -= 10.0
        }
        
        // 패널티 3: 예산 임계
        if (isBudgetMarginal(preference, program, school)) {
            penalty -= 10.0
        }
        
        // 패널티 4: 체류 의사 불명확
        if (isOptIntentionUnclear(preference)) {
            penalty -= 5.0
        }
        
        return penalty
    }
    
    /**
     * 입학 경쟁률 < 30% (합격률 기준)
     */
    private fun isHighCompetition(school: School): Boolean {
        return (school.acceptanceRate ?: 100) < 30
    }
    
    /**
     * 영어 점수 여유 < 5점
     */
    private fun isEnglishScoreMarginal(profile: AcademicProfile, program: Program): Boolean {
        val userScore = EnglishScoreConverter.convertToToefl(
            testType = profile.englishTestType,
            score = profile.englishScore
        )
        
        val minRequired = when (program.type.lowercase()) {
            "university" -> 80
            "community_college" -> 61
            "vocational" -> 45
            else -> 60
        }
        
        val surplus = userScore - minRequired
        return surplus in 0..5
    }
    
    /**
     * 예산 여유 < $3,000
     */
    private fun isBudgetMarginal(
        preference: UserPreference,
        program: Program,
        school: School
    ): Boolean {
        val userBudget = preference.budgetUsd ?: return false
        val tuition = program.tuition ?: school.tuition ?: 0
        val livingCost = school.livingCost ?: 0
        val totalCost = tuition + livingCost
        
        val surplus = userBudget - totalCost
        return surplus in 0..3000
    }
    
    /**
     * OPT 의사 불명확
     */
    private fun isOptIntentionUnclear(preference: UserPreference): Boolean {
        val careerGoal = preference.careerGoal ?: ""
        val hasOptKeyword = careerGoal.contains("취업") || 
                           careerGoal.contains("OPT") ||
                           preference.preferredTrack?.contains("취업") == true
        
        return !hasOptKeyword
    }
}
