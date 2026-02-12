package com.goalmond.api.service.matching

import com.goalmond.api.domain.entity.AcademicProfile
import com.goalmond.api.domain.entity.Program
import com.goalmond.api.domain.entity.School
import com.goalmond.api.domain.entity.UserPreference
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 매칭 설명 생성 서비스 (GAM-3, Phase 7 + RAG 통합).
 * 
 * Spring AI RAGService를 활용하여 문서 기반 설명을 생성합니다.
 * RAG 실패 시 템플릿 기반 설명으로 Fallback합니다.
 */
@Service
class ExplanationService(
    private val ragService: RAGService
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    /**
     * 매칭 설명 생성.
     * 
     * Spring AI RAG를 사용하여 관련 문서를 검색하고 근거 있는 설명을 생성합니다.
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
            // Spring AI RAG 사용 (문서 검색 + 생성)
            ragService.generateExplanationWithRAG(profile, preference, program, school, scores)
        } catch (e: Exception) {
            logger.warn("RAG explanation failed for ${school.name}, using template fallback", e)
            generateTemplateExplanation(program, school, scores)
        }
    }
    
    /**
     * Fallback: 템플릿 기반 설명 생성.
     * 
     * RAG 실패 시 또는 문서가 없을 때 사용합니다.
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

        school.employmentRate?.let {
            pros.add("취업률 정보 제공 (${it}%)")
        }

        if (!school.internationalEmail.isNullOrBlank()) {
            pros.add("유학생 담당 연락 채널 확인 가능")
        }

        if (!school.eslProgram.isNullOrBlank() && school.eslProgram!!.contains("true", ignoreCase = true)) {
            pros.add("ESL 지원 정보 확인 가능")
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

        if (school.internationalEmail.isNullOrBlank()) {
            cons.add("유학생 전용 연락처 정보가 부족할 수 있음")
        }
        
        return Pair(pros, cons)
    }
}
