package com.goalmond.api.service.matching

import com.goalmond.api.domain.entity.AcademicProfile
import com.goalmond.api.domain.entity.Program
import com.goalmond.api.domain.entity.School
import com.goalmond.api.domain.entity.UserPreference
import com.goalmond.api.domain.util.GpaConverter
import org.springframework.stereotype.Service

/**
 * 경로 최적화 서비스 (GAM-3, Phase 6).
 * 
 * 사용자 상황에 맞는 최적 경로에 가점을 부여합니다.
 * 
 * 4가지 시나리오:
 * 1. GPA 낮음 + 예산 제한 → CC +10점
 * 2. 영어 없음 + 취업 목표 → Vocational +15점
 * 3. 편입 목표 + 편입률 높음 → CC +10점
 * 4. OPT 의사 + OPT 가능 → +5점
 */
@Service
class PathOptimizationService {
    
    /**
     * 경로 최적화 가점 계산.
     * 
     * @return 가점 (0 ~ 30)
     */
    fun applyOptimization(
        profile: AcademicProfile,
        preference: UserPreference,
        program: Program,
        school: School
    ): Double {
        var boost = 0.0
        
        // 시나리오 1: GPA 낮음 + 예산 제한 → CC 추천
        if (isLowGpaWithBudgetLimit(profile, preference) && program.type.lowercase() == "community_college") {
            boost += 10.0
        }
        
        // 시나리오 2: 영어 없음 + 취업 목표 → Vocational 추천
        if (isNoEnglishWithCareerGoal(profile, preference) && program.type.lowercase() == "vocational") {
            boost += 15.0
        }
        
        // 시나리오 3: 편입 목표 + 편입률 높음 → CC 추천
        if (isTransferGoalWithHighRate(preference, school) && program.type.lowercase() == "community_college") {
            boost += 10.0
        }
        
        // 시나리오 4: OPT 의사 + OPT 가능 → 가점
        if (hasOptIntention(preference) && program.optAvailable) {
            boost += 5.0
        }
        
        return boost
    }
    
    private fun isLowGpaWithBudgetLimit(profile: AcademicProfile, preference: UserPreference): Boolean {
        val gpa = GpaConverter.normalize(profile.gpa, profile.gpaScale)
        val budget = preference.budgetUsd ?: Int.MAX_VALUE
        return gpa < 3.0 && budget < 30000
    }
    
    private fun isNoEnglishWithCareerGoal(profile: AcademicProfile, preference: UserPreference): Boolean {
        val hasEnglish = profile.englishScore != null && profile.englishScore!! > 0
        val hasCareerGoal = preference.careerGoal?.isNotEmpty() == true
        return !hasEnglish && hasCareerGoal
    }
    
    private fun isTransferGoalWithHighRate(preference: UserPreference, school: School): Boolean {
        val hasTransferGoal = preference.preferredTrack?.contains("편입") == true
        val highTransferRate = (school.transferRate ?: 0) >= 60
        return hasTransferGoal && highTransferRate
    }
    
    private fun hasOptIntention(preference: UserPreference): Boolean {
        return preference.careerGoal?.lowercase()?.contains("취업") == true ||
               preference.preferredTrack?.contains("취업") == true
    }
}
