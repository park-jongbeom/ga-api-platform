package com.goalmond.api.service.matching

import com.goalmond.api.domain.entity.AcademicProfile
import com.goalmond.api.domain.entity.Program
import com.goalmond.api.domain.entity.School
import com.goalmond.api.domain.entity.UserPreference
import com.goalmond.api.service.ai.GeminiClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 매칭 설명 생성 서비스 (GAM-3, Phase 7).
 * 
 * Gemini AI를 활용하여 각 추천 학교에 대한 설명을 생성합니다.
 */
@Service
class ExplanationService(
    private val geminiClient: GeminiClient
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    /**
     * 매칭 설명 생성.
     * 
     * @return 2-3문장의 설명
     */
    fun generateExplanation(
        profile: AcademicProfile,
        preference: UserPreference,
        program: Program,
        school: School,
        scores: ScoreBreakdown
    ): String {
        return try {
            val prompt = buildPrompt(profile, preference, program, school, scores)
            geminiClient.generateContent(prompt)
        } catch (e: Exception) {
            logger.warn("Gemini explanation failed, using template", e)
            generateTemplateExplanation(program, school, scores)
        }
    }
    
    /**
     * Gemini 프롬프트 생성.
     */
    private fun buildPrompt(
        profile: AcademicProfile,
        preference: UserPreference,
        program: Program,
        school: School,
        scores: ScoreBreakdown
    ): String {
        return """
당신은 유학 매칭 전문가입니다. 다음 정보를 바탕으로 학생에게 이 학교를 추천하는 이유를 2-3문장으로 설명해주세요.

학생 정보:
- GPA: ${profile.gpa} / ${profile.gpaScale}
- 영어 점수: ${profile.englishScore} (${profile.englishTestType})
- 예산: $${preference.budgetUsd}/년
- 목표 전공: ${preference.targetMajor}
- 커리어 목표: ${preference.careerGoal}

학교 정보:
- 이름: ${school.name}
- 유형: ${program.type}
- 학비: $${program.tuition ?: school.tuition}/년
- 지역: ${school.city}, ${school.state}
- 편입률: ${school.transferRate}%
- 합격률: ${school.acceptanceRate}%

매칭 점수:
- 총점: ${String.format("%.1f", scores.total())}점
- 학업: ${String.format("%.1f", scores.academic)}점
- 영어: ${String.format("%.1f", scores.english)}점
- 예산: ${String.format("%.1f", scores.budget)}점
- 진로: ${String.format("%.1f", scores.career)}점
- 지역: ${String.format("%.1f", scores.location)}점

설명은 친근하고 구체적으로, "이 학교는..."으로 시작해주세요. 2-3문장 이내로 작성하세요.
        """.trimIndent()
    }
    
    /**
     * Fallback: 템플릿 기반 설명 생성.
     */
    private fun generateTemplateExplanation(
        program: Program,
        school: School,
        scores: ScoreBreakdown
    ): String {
        val strengths = mutableListOf<String>()
        
        if (scores.budget > 10) strengths.add("예산 대비 학비가 적절하며")
        if (scores.english > 10) strengths.add("영어 점수가 입학 기준을 충족하고")
        if (scores.career > 20) strengths.add("희망 진로와 연계성이 높아")
        
        val strengthText = if (strengths.isNotEmpty()) {
            strengths.joinToString(", ") + " 추천되었습니다."
        } else {
            "전반적으로 적합한 프로그램입니다."
        }
        
        return "이 학교는 ${school.city}, ${school.state}에 위치한 ${program.type} 프로그램으로, $strengthText"
    }
    
    /**
     * 장단점 자동 생성.
     */
    fun generateProsAndCons(
        profile: AcademicProfile,
        preference: UserPreference,
        program: Program,
        school: School,
        scores: ScoreBreakdown
    ): Pair<List<String>, List<String>> {
        val pros = mutableListOf<String>()
        val cons = mutableListOf<String>()
        
        // Pros
        val budgetSurplus = (preference.budgetUsd ?: 0) - (program.tuition ?: 0) - (school.livingCost ?: 0)
        if (budgetSurplus > 5000) {
            pros.add("예산 여유 충분 (\$${budgetSurplus})")
        }
        
        if (scores.english > 10) {
            pros.add("영어 점수 입학 기준 초과")
        }
        
        if (program.optAvailable) {
            pros.add("OPT 가능")
        }
        
        if ((school.transferRate ?: 0) > 65) {
            pros.add("높은 편입 성공률 (${school.transferRate}%)")
        }
        
        // Cons
        if ((school.acceptanceRate ?: 100) < 40) {
            cons.add("경쟁률 다소 높음 (${school.acceptanceRate}%)")
        }
        
        if (budgetSurplus < 3000) {
            cons.add("예산 임계")
        }
        
        if (scores.location < 5) {
            cons.add("선호 지역과 거리 있음")
        }
        
        return Pair(pros, cons)
    }
}
