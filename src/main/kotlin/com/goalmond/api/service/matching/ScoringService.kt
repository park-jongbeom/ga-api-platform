package com.goalmond.api.service.matching

import com.goalmond.api.config.WeightConfig
import com.goalmond.api.domain.entity.AcademicProfile
import com.goalmond.api.domain.entity.Program
import com.goalmond.api.domain.entity.School
import com.goalmond.api.domain.entity.UserPreference
import com.goalmond.api.domain.util.EnglishScoreConverter
import com.goalmond.api.domain.util.GpaConverter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.math.max

/**
 * 매칭 점수 계산 서비스 (GAM-3, Phase 5).
 * 
 * 6대 지표를 기반으로 Base Score를 계산합니다:
 * 1. 학업 적합도 (20%)
 * 2. 영어 적합도 (15%)
 * 3. 예산 적합도 (15%)
 * 4. 지역 선호 (10%)
 * 5. 기간 적합도 (10%)
 * 6. 진로 연계성 (30%)
 * 
 * 총점: 100점
 */
@Service
class ScoringService(
    private val weightConfig: WeightConfig
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    /**
     * 6대 지표 점수 계산.
     * 
     * @return ScoreBreakdown (각 지표별 점수)
     */
    fun calculateScore(
        profile: AcademicProfile,
        preference: UserPreference,
        program: Program,
        school: School
    ): ScoreBreakdown {
        val academic = calculateAcademicScore(profile, program) * weightConfig.academic * 100
        val english = calculateEnglishScore(profile, program) * weightConfig.english * 100
        val budget = calculateBudgetScore(preference, program, school) * weightConfig.budget * 100
        val location = calculateLocationScore(preference, school) * weightConfig.location * 100
        val duration = calculateDurationScore(preference, program) * weightConfig.duration * 100
        val career = calculateCareerScore(preference, program) * weightConfig.career * 100
        
        return ScoreBreakdown(
            academic = academic,
            english = english,
            budget = budget,
            location = location,
            duration = duration,
            career = career
        )
    }
    
    /**
     * 1. 학업 적합도 (Academic Fit).
     * 
     * 계산: (사용자 GPA - 프로그램 최소 GPA) / 4.0
     * 
     * 프로그램 최소 GPA 기본값:
     * - university: 3.0
     * - community_college: 2.0
     * - vocational: 없음 (1.0)
     */
    private fun calculateAcademicScore(profile: AcademicProfile, program: Program): Double {
        val userGpa = GpaConverter.normalize(profile.gpa, profile.gpaScale)
        
        val minGpa = when (program.type.lowercase()) {
            "university" -> 3.0
            "community_college" -> 2.0
            "vocational" -> 1.0
            else -> 2.5
        }
        
        // GPA가 최소 요건을 초과한 정도
        val surplus = (userGpa - minGpa).coerceAtLeast(0.0)
        
        // 4.0 만점 대비 비율
        return (surplus / 4.0).coerceIn(0.0, 1.0)
    }
    
    /**
     * 2. 영어 적합도 (English Proficiency).
     * 
     * 계산: (사용자 점수 - 최소 요구) / 120
     */
    private fun calculateEnglishScore(profile: AcademicProfile, program: Program): Double {
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
        
        val surplus = (userScore - minRequired).coerceAtLeast(0)
        
        // TOEFL 120 만점 대비 여유도
        return (surplus.toDouble() / 120.0).coerceIn(0.0, 1.0)
    }
    
    /**
     * 3. 예산 적합도 (Budget Fit).
     * 
     * 계산: (사용자 예산 - 총 비용) / 사용자 예산
     */
    private fun calculateBudgetScore(
        preference: UserPreference,
        program: Program,
        school: School
    ): Double {
        val userBudget = preference.budgetUsd ?: return 0.5 // 예산 정보 없으면 중립
        
        val tuition = program.tuition ?: school.tuition ?: 0
        val livingCost = school.livingCost ?: 0
        val totalCost = tuition + livingCost
        
        val surplus = (userBudget - totalCost).coerceAtLeast(0)
        
        // 예산 대비 여유도
        return (surplus.toDouble() / userBudget).coerceIn(0.0, 1.0)
    }
    
    /**
     * 4. 지역 선호 (Location Preference).
     * 
     * 계산:
     * - 희망 주/도시 정확히 일치: 1.0
     * - 희망 주만 일치: 0.7
     * - 불일치: 0.0
     */
    private fun calculateLocationScore(preference: UserPreference, school: School): Double {
        val targetLocation = preference.targetLocation?.lowercase() ?: return 0.5 // 선호 없으면 중립
        
        val schoolState = school.state?.lowercase() ?: ""
        val schoolCity = school.city?.lowercase() ?: ""
        
        return when {
            // 도시명 포함
            targetLocation.contains(schoolCity) && schoolCity.isNotEmpty() -> 1.0
            // 주명 포함
            targetLocation.contains(schoolState) && schoolState.isNotEmpty() -> 0.7
            schoolState.contains(targetLocation) -> 0.7
            else -> 0.0
        }
    }
    
    /**
     * 5. 기간 적합도 (Duration Fit).
     * 
     * 계산:
     * - 목표 기간과 프로그램 기간 일치: 1.0
     * - ±6개월: 0.7
     * - 차이 크면: 0.3
     * 
     * 참고: 현재 UserPreference에 target_duration 필드 없음.
     * 프로그램 유형으로 추론:
     * - 2년제 선호 → community_college
     * - 4년제 선호 → university
     */
    private fun calculateDurationScore(preference: UserPreference, program: Program): Double {
        val targetProgram = preference.targetProgram?.lowercase()
        val programType = program.type.lowercase()
        
        return when {
            targetProgram == programType -> 1.0
            targetProgram == "community_college" && programType == "vocational" -> 0.7
            targetProgram == "university" && programType == "community_college" -> 0.5 // 편입 가능
            else -> 0.3
        }
    }
    
    /**
     * 6. 진로 연계성 (Career Alignment).
     * 
     * 계산: 목표 전공/직업과 프로그램 전공/진로 키워드 일치도
     * 
     * 키워드 기반 유사도:
     * - targetMajor와 program.name 키워드 매칭
     * - careerGoal과 program 연계성 (간단한 키워드 매칭)
     */
    private fun calculateCareerScore(preference: UserPreference, program: Program): Double {
        val targetMajor = preference.targetMajor?.lowercase() ?: ""
        val careerGoal = preference.careerGoal?.lowercase() ?: ""
        val programName = program.name.lowercase()
        
        var score = 0.0
        
        // 전공명 일치도
        if (targetMajor.isNotEmpty()) {
            val majorKeywords = targetMajor.split(" ", ",", "/")
            val matchCount = majorKeywords.count { keyword ->
                keyword.length > 2 && programName.contains(keyword)
            }
            score += if (matchCount > 0) 0.6 else 0.0
        }
        
        // 커리어 키워드 일치도
        if (careerGoal.isNotEmpty()) {
            val careerKeywords = listOf("engineer", "developer", "designer", "analyst", "manager")
            val hasCareerKeyword = careerKeywords.any { careerGoal.contains(it) && programName.contains(it) }
            score += if (hasCareerKeyword) 0.4 else 0.1
        }
        
        return score.coerceIn(0.0, 1.0)
    }
}

/**
 * 6대 지표 점수 DTO.
 */
data class ScoreBreakdown(
    val academic: Double,    // 학업 적합도 (0-20)
    val english: Double,     // 영어 적합도 (0-15)
    val budget: Double,      // 예산 적합도 (0-15)
    val location: Double,    // 지역 선호 (0-10)
    val duration: Double,    // 기간 적합도 (0-10)
    val career: Double       // 진로 연계성 (0-30)
) {
    /**
     * 총점 계산.
     */
    fun total(): Double = academic + english + budget + location + duration + career
}
