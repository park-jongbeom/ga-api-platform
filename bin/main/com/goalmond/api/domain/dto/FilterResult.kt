package com.goalmond.api.domain.dto

import com.goalmond.api.domain.entity.Program

/**
 * Hard Filter 결과 (GAM-3, Phase 4).
 */
data class FilterResult(
    val passed: List<Program>,
    val filtered: List<FilteredProgram>
) {
    fun passedCount(): Int = passed.size
    fun filteredCount(): Int = filtered.size
}

/**
 * 필터링된 프로그램 정보.
 */
data class FilteredProgram(
    val program: Program,
    val reason: String,
    val filterType: FilterType
)

/**
 * 필터 유형.
 */
enum class FilterType {
    BUDGET_EXCEEDED,      // 예산 초과
    VISA_REQUIREMENT,     // 비자 요건 불충족
    ENGLISH_SCORE,        // 영어 점수 미달
    ADMISSION_SEASON      // 입학 시기 불일치
}
