package com.goalmond.api.service.matching

import com.goalmond.api.domain.dto.FilterResult
import com.goalmond.api.domain.dto.FilterType
import com.goalmond.api.domain.dto.FilteredProgram
import com.goalmond.api.domain.entity.AcademicProfile
import com.goalmond.api.domain.entity.Program
import com.goalmond.api.domain.entity.School
import com.goalmond.api.domain.entity.UserPreference
import com.goalmond.api.domain.util.EnglishScoreConverter
import com.goalmond.api.repository.SchoolRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Hard Filter 서비스 (GAM-3, Phase 4).
 * 
 * 지원 불가능한 프로그램을 사전에 제거합니다.
 * 
 * 4가지 필터:
 * 1. 예산 초과
 * 2. 비자 요건 불충족
 * 3. 영어 점수 미달
 * 4. 입학 시기 불일치
 */
@Service
class HardFilterService(
    private val schoolRepository: SchoolRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    /**
     * 프로그램 목록을 필터링.
     * 
     * @param profile 학력 프로필
     * @param preference 선호도
     * @param programs 필터링 대상 프로그램 목록
     * @return 필터 결과 (통과/필터링)
     */
    fun filterPrograms(
        profile: AcademicProfile,
        preference: UserPreference,
        programs: List<Program>
    ): FilterResult {
        val passed = mutableListOf<Program>()
        val filtered = mutableListOf<FilteredProgram>()
        
        // School 정보를 한 번에 로드 (N+1 쿼리 방지)
        val schoolIds = programs.mapNotNull { it.schoolId }.toSet()
        val schools = schoolRepository.findAllById(schoolIds).associateBy { it.id }
        
        programs.forEach { program ->
            val school = schools[program.schoolId]
            val filterReason = checkFilters(profile, preference, program, school)
            
            if (filterReason == null) {
                passed.add(program)
            } else {
                filtered.add(filterReason)
            }
        }
        
        logger.info(
            "Hard filter completed: ${passed.size} passed, ${filtered.size} filtered " +
            "(budget=${filtered.count { it.filterType == FilterType.BUDGET_EXCEEDED}}, " +
            "visa=${filtered.count { it.filterType == FilterType.VISA_REQUIREMENT}}, " +
            "english=${filtered.count { it.filterType == FilterType.ENGLISH_SCORE}}, " +
            "season=${filtered.count { it.filterType == FilterType.ADMISSION_SEASON}})"
        )
        
        return FilterResult(passed, filtered)
    }
    
    /**
     * 4가지 필터 검사.
     * 
     * @return 필터링 사유 (null이면 통과)
     */
    private fun checkFilters(
        profile: AcademicProfile,
        preference: UserPreference,
        program: Program,
        school: School?
    ): FilteredProgram? {
        // 1. 예산 초과 필터
        val budgetExceeded = checkBudgetFilter(preference, program, school)
        if (budgetExceeded != null) return budgetExceeded
        
        // 2. 비자 요건 필터 (초등 프로그램은 성인 불가)
        val visaFailed = checkVisaFilter(program)
        if (visaFailed != null) return visaFailed
        
        // 3. 영어 점수 미달 필터
        val englishFailed = checkEnglishFilter(profile, program)
        if (englishFailed != null) return englishFailed
        
        // 4. 입학 시기 필터 (현재는 Skip - 입학 시기 정보 없음)
        // val seasonFailed = checkAdmissionSeasonFilter(preference, program)
        // if (seasonFailed != null) return seasonFailed
        
        return null
    }
    
    /**
     * 필터 1: 예산 초과.
     * 
     * 조건: 프로그램 학비 + 생활비 > 사용자 예산
     */
    private fun checkBudgetFilter(
        preference: UserPreference,
        program: Program,
        school: School?
    ): FilteredProgram? {
        val userBudget = preference.budgetUsd ?: return null // 예산 정보 없으면 Skip
        
        val tuition = program.tuition ?: school?.tuition ?: 0
        val livingCost = school?.livingCost ?: 0
        val totalCost = tuition + livingCost
        
        return if (totalCost > userBudget) {
            FilteredProgram(
                program = program,
                reason = "예산 초과: 총 비용 \$$totalCost > 예산 \$$userBudget",
                filterType = FilterType.BUDGET_EXCEEDED
            )
        } else null
    }
    
    /**
     * 필터 2: 비자 요건.
     * 
     * 조건: 초등 프로그램은 성인(18세 이상) 불가
     */
    private fun checkVisaFilter(program: Program): FilteredProgram? {
        return if (program.type.lowercase() == "elementary") {
            FilteredProgram(
                program = program,
                reason = "비자 요건: 초등 프로그램은 성인 지원 불가",
                filterType = FilterType.VISA_REQUIREMENT
            )
        } else null
    }
    
    /**
     * 필터 3: 영어 점수 미달.
     * 
     * 조건: 환산 점수 < 프로그램 최소 요구 점수
     * 
     * 참고: 현재 Program 엔티티에 minEnglishScore 필드가 없으므로
     * 프로그램 유형별 기본값 사용:
     * - university: TOEFL 80
     * - community_college: TOEFL 61 (IELTS 6.0)
     * - vocational: TOEFL 45 (IELTS 5.5)
     */
    private fun checkEnglishFilter(
        profile: AcademicProfile,
        program: Program
    ): FilteredProgram? {
        val userScore = profile.englishScore ?: return null // 영어 점수 없으면 Skip
        
        // TOEFL로 환산
        val toeflScore = EnglishScoreConverter.convertToToefl(
            testType = profile.englishTestType,
            score = userScore
        )
        
        // 프로그램 유형별 최소 요구 점수
        val minRequired = when (program.type.lowercase()) {
            "university" -> 80
            "community_college" -> 61
            "vocational" -> 45
            "elementary" -> 0
            else -> 60
        }
        
        return if (toeflScore < minRequired) {
            FilteredProgram(
                program = program,
                reason = "영어 점수 미달: ${profile.englishTestType} $userScore (TOEFL 환산 $toeflScore) < 요구 TOEFL $minRequired",
                filterType = FilterType.ENGLISH_SCORE
            )
        } else null
    }
}
