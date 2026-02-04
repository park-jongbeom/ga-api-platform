package com.goalmond.api.service.matching

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.goalmond.api.domain.dto.MatchingResponse
import com.goalmond.api.domain.entity.School
import com.goalmond.api.domain.entity.UserPreference
import com.goalmond.api.repository.AcademicProfileRepository
import com.goalmond.api.repository.ProgramRepository
import com.goalmond.api.repository.UserPreferenceRepository
import com.goalmond.api.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

/**
 * 매칭 엔진 서비스 (GAM-3, Phase 8).
 * 
 * RAG + Rule-based 하이브리드 매칭 엔진:
 * 1. 사용자 데이터 로드
 * 2. RAG 벡터 검색 (Top 20)
 * 3. Hard Filter
 * 4. Scoring (6대 지표)
 * 5. Path Optimization
 * 6. Risk Penalty
 * 7. Top 5 선정
 * 8. 설명 생성
 */
@Service
class MatchingEngineService(
    private val userRepository: UserRepository,
    private val academicProfileRepository: AcademicProfileRepository,
    private val userPreferenceRepository: UserPreferenceRepository,
    private val programRepository: ProgramRepository,
    private val vectorSearchService: VectorSearchService,
    private val hardFilterService: HardFilterService,
    private val scoringService: ScoringService,
    private val pathOptimizationService: PathOptimizationService,
    private val riskPenaltyService: RiskPenaltyService,
    private val explanationService: ExplanationService,
    private val fallbackMatchingService: FallbackMatchingService
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    /**
     * 매칭 실행.
     * 
     * @param userId 사용자 ID
     * @return 매칭 결과
     */
    @Transactional(readOnly = true)
    fun executeMatching(userId: UUID): MatchingResponse {
        val startTime = System.currentTimeMillis()
        
        try {
            // Step 1: 사용자 데이터 로드
            val user = userRepository.findById(userId).orElseThrow {
                MatchingException("User not found: $userId")
            }
            val profile = academicProfileRepository.findByUserId(userId)
                ?: throw MatchingException("Academic profile not found for user: $userId")
            val preference = userPreferenceRepository.findByUserId(userId)
                ?: throw MatchingException("User preference not found for user: $userId")
            
            logger.info("매칭 시작: user=$userId, major=${preference.targetMajor}, budget=${preference.budgetUsd}")
            
            // Step 2: RAG 벡터 검색 (Top 20)
            val candidateSchools = vectorSearchService.searchSimilarSchools(user, profile, preference)
            logger.info("벡터 검색 완료: ${candidateSchools.size}개 후보군")
            
            // Fallback: DB에 학교/임베딩 데이터가 없으면 프로필·선호도만으로 Gemini 추천 생성
            if (candidateSchools.isEmpty()) {
                logger.info("후보군 없음 → Fallback(AI 추천) 실행")
                val fallbackResults = fallbackMatchingService.generateFallbackResults(profile, preference)
                val fallbackTime = System.currentTimeMillis() - startTime
                return MatchingResponse(
                    matchingId = UUID.randomUUID().toString(),
                    userId = userId.toString(),
                    totalMatches = fallbackResults.size,
                    executionTimeMs = fallbackTime.toInt(),
                    results = fallbackResults,
                    createdAt = Instant.now(),
                    message = "DB에 데이터가 없어 API 정보만으로 생성한 추천입니다. 실제 DB 데이터와 무관할 수 있습니다."
                )
            }
            
            // Step 3: School에 연결된 Program 조회
            val schoolIds = candidateSchools.mapNotNull { it.id }
            val programs = programRepository.findBySchoolIdIn(schoolIds)
            logger.info("프로그램 조회: ${programs.size}개")
            
            // Step 4: Hard Filter
            val filterResult = hardFilterService.filterPrograms(profile, preference, programs)
            logger.info("Hard Filter: ${filterResult.passedCount()}개 통과, ${filterResult.filteredCount()}개 필터링")
            
            // Fallback: Hard Filter 통과 결과가 없으면 AI 추천 생성 (하이브리드 방식)
            if (filterResult.passed.isEmpty()) {
                logger.info("Hard Filter 통과 결과 없음 → Fallback(AI 추천) 실행")
                
                // 필터링 통계 수집
                val budgetFiltered = filterResult.filtered.count { it.filterType == com.goalmond.api.domain.dto.FilterType.BUDGET_EXCEEDED }
                val englishFiltered = filterResult.filtered.count { it.filterType == com.goalmond.api.domain.dto.FilterType.ENGLISH_SCORE }
                val visaFiltered = filterResult.filtered.count { it.filterType == com.goalmond.api.domain.dto.FilterType.VISA_REQUIREMENT }
                
                // 최저 학비 찾기 (프로그램 또는 학교 학비 중 최소값)
                val minTuition = programs.mapNotNull { program ->
                    program.tuition ?: candidateSchools.find { it.id == program.schoolId }?.tuition
                }.minOrNull()
                
                // 상세 메시지 생성
                val message = buildFilteredMessage(preference, budgetFiltered, englishFiltered, minTuition)
                
                val fallbackResults = fallbackMatchingService.generateFallbackResults(profile, preference)
                val fallbackTime = System.currentTimeMillis() - startTime
                
                return MatchingResponse(
                    matchingId = UUID.randomUUID().toString(),
                    userId = userId.toString(),
                    totalMatches = fallbackResults.size,
                    executionTimeMs = fallbackTime.toInt(),
                    results = fallbackResults,
                    createdAt = Instant.now(),
                    message = message,
                    filterSummary = MatchingResponse.FilterSummary(
                        totalCandidates = filterResult.filtered.size,
                        filteredByBudget = budgetFiltered,
                        filteredByEnglish = englishFiltered,
                        filteredByVisa = visaFiltered,
                        minimumTuitionFound = minTuition
                    )
                )
            }
            
            // Step 5: 점수 계산 및 최종 점수 산출
            val candidates = filterResult.passed.mapNotNull { program ->
                val school = candidateSchools.find { it.id == program.schoolId } ?: return@mapNotNull null
                
                // 6대 지표 Base Score
                val scores = scoringService.calculateScore(profile, preference, program, school)
                
                // 경로 최적화 가점
                val optimization = pathOptimizationService.applyOptimization(profile, preference, program, school)
                
                // 리스크 패널티
                val penalty = riskPenaltyService.applyPenalty(profile, preference, program, school)
                
                // 최종 점수
                val totalScore = scores.total() + optimization + penalty
                
                MatchingCandidate(
                    program = program,
                    school = school,
                    scores = scores,
                    optimization = optimization,
                    penalty = penalty,
                    totalScore = totalScore.coerceAtLeast(0.0)
                )
            }
            
            // Step 6: Top 5 선정 (최종 점수 내림차순)
            val top5 = candidates
                .sortedByDescending { it.totalScore }
                .take(5)
            
            logger.info("Top 5 선정 완료: 점수 범위 ${top5.firstOrNull()?.totalScore} ~ ${top5.lastOrNull()?.totalScore}")
            
            // Step 7: 설명 생성 및 추천 유형 분류
            val results = top5.mapIndexed { index, candidate ->
                val (pros, cons) = explanationService.generateProsAndCons(
                    profile, preference, candidate.program, candidate.school, candidate.scores
                )
                
                val recommendationType = classifyRecommendationType(candidate.totalScore)
                
                // Gemini 설명 생성은 Quota 이슈로 인해 템플릿 사용
                val explanation = generateSimpleExplanation(candidate, recommendationType)
                
                MatchingResponse.MatchingResult(
                    rank = index + 1,
                    school = buildSchoolSummary(candidate.school),
                    program = buildProgramSummary(candidate.program),
                    totalScore = candidate.totalScore,
                    estimatedRoi = calculateEstimatedRoi(candidate.school, candidate.program),
                    scoreBreakdown = buildScoreBreakdown(candidate.scores),
                    recommendationType = recommendationType,
                    explanation = explanation,
                    pros = pros,
                    cons = cons
                )
            }
            
            val executionTime = System.currentTimeMillis() - startTime
            
            return MatchingResponse(
                matchingId = UUID.randomUUID().toString(),
                userId = userId.toString(),
                totalMatches = results.size,
                executionTimeMs = executionTime.toInt(),
                results = results,
                createdAt = Instant.now()
            )
            
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startTime
            logger.error("매칭 실패 (${elapsed}ms)", e)
            throw MatchingException("Matching failed for user $userId", e)
        }
    }
    
    /**
     * Hard Filter에서 모두 필터링되었을 때 상세 메시지 생성 (하이브리드 방식).
     * 
     * @param preference 사용자 선호도
     * @param budgetFiltered 예산 초과로 필터링된 수
     * @param englishFiltered 영어 점수 미달로 필터링된 수
     * @param minTuition 후보 중 최저 학비
     * @return 사용자에게 보여줄 상세 안내 메시지
     */
    private fun buildFilteredMessage(
        preference: UserPreference,
        budgetFiltered: Int,
        englishFiltered: Int,
        minTuition: Int?
    ): String {
        val reasons = mutableListOf<String>()
        
        if (budgetFiltered > 0) {
            val budget = preference.budgetUsd ?: 0
            val suggestion = if (minTuition != null && minTuition > budget) {
                " (최저 학비: \$$minTuition)"
            } else ""
            reasons.add("• ${budgetFiltered}개 학교 예산 초과$suggestion")
        }
        
        if (englishFiltered > 0) {
            reasons.add("• ${englishFiltered}개 학교 영어 점수 미달")
        }
        
        val reasonText = if (reasons.isNotEmpty()) {
            "\n\n필터링 이유:\n${reasons.joinToString("\n")}"
        } else ""
        
        return "⚠️ 입력하신 조건으로는 적합한 학교가 없어 AI 추천을 제공합니다.$reasonText\n\n아래는 조건을 완화한 추천입니다."
    }
    
    /**
     * 추천 유형 분류.
     */
    private fun classifyRecommendationType(score: Double): String {
        return when {
            score >= 85 -> "safe"
            score >= 70 -> "challenge"
            score >= 60 -> "strategy"
            else -> "strategy"
        }
    }
    
    /**
     * 간단한 템플릿 설명 생성.
     */
    private fun generateSimpleExplanation(candidate: MatchingCandidate, type: String): String {
        val school = candidate.school
        return when (type) {
            "safe" -> "이 학교는 예산 대비 학비가 안정적이며, 귀하의 영어 점수로 바로 입학이 가능하고, 졸업 후 OPT 연계 확률이 높아 추천되었습니다."
            "challenge" -> "이 학교는 ${school.city}에 위치하며 편입률이 높아 장기적으로 유리하지만, 경쟁률을 고려하여 도전해볼 만한 선택지입니다."
            else -> "이 학교는 예산과 목표에 맞춰 전략적으로 선택할 수 있는 프로그램입니다."
        }
    }
    
    private fun buildSchoolSummary(school: School): MatchingResponse.SchoolSummary {
        val tuition = school.tuition ?: 0
        return MatchingResponse.SchoolSummary(
            id = school.id.toString(),
            name = school.name,
            type = school.type,
            state = school.state ?: "",
            city = school.city ?: "",
            tuition = tuition,
            imageUrl = "https://cdn.goalmond.com/schools/${school.id}.jpg",
            globalRanking = school.globalRanking,
            rankingField = school.rankingField,
            averageSalary = school.averageSalary,
            alumniNetworkCount = school.alumniNetworkCount,
            featureBadges = parseFeatureBadges(school.featureBadges)
        )
    }

    /**
     * feature_badges JSON 문자열을 List로 파싱. 실패 시 빈 리스트.
     */
    private fun parseFeatureBadges(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            jacksonObjectMapper().readValue<List<String>>(raw)
        } catch (e: Exception) {
            logger.debug("feature_badges 파싱 실패: {}", raw, e)
            emptyList()
        }
    }

    /**
     * 연간 예상 ROI (%): (average_salary - tuition) / tuition * 100.
     * null 또는 tuition 0이면 0.0, 음수면 0.0.
     */
    private fun calculateEstimatedRoi(school: School, program: com.goalmond.api.domain.entity.Program): Double {
        val salary = school.averageSalary ?: return 0.0
        val tuition = school.tuition ?: program.tuition ?: return 0.0
        if (tuition <= 0) return 0.0
        val roi = (salary - tuition).toDouble() / tuition * 100
        return roi.coerceAtLeast(0.0)
    }
    
    private fun buildProgramSummary(program: com.goalmond.api.domain.entity.Program) = 
        MatchingResponse.ProgramSummary(
            id = program.id.toString(),
            name = program.name,
            degree = program.degree ?: "",
            duration = program.duration ?: "",
            optAvailable = program.optAvailable
        )
    
    private fun buildScoreBreakdown(scores: ScoreBreakdown) = 
        MatchingResponse.ScoreBreakdown(
            academic = scores.academic.toInt(),
            english = scores.english.toInt(),
            budget = scores.budget.toInt(),
            location = scores.location.toInt(),
            duration = scores.duration.toInt(),
            career = scores.career.toInt()
        )
}

/**
 * 매칭 후보 (내부용).
 */
data class MatchingCandidate(
    val program: com.goalmond.api.domain.entity.Program,
    val school: com.goalmond.api.domain.entity.School,
    val scores: ScoreBreakdown,
    val optimization: Double,
    val penalty: Double,
    val totalScore: Double
)

/**
 * 매칭 예외.
 */
class MatchingException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
