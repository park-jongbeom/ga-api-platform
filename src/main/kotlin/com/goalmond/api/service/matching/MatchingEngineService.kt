package com.goalmond.api.service.matching

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.goalmond.api.domain.dto.MatchingResponse
import com.goalmond.api.domain.entity.School
import com.goalmond.api.domain.entity.UserPreference
import com.goalmond.api.repository.AcademicProfileRepository
import com.goalmond.api.repository.GraphRagEntityRepository
import com.goalmond.api.repository.ProgramRepository
import com.goalmond.api.repository.UserPreferenceRepository
import com.goalmond.api.repository.UserRepository
import com.goalmond.api.domain.graphrag.EntityType
import com.goalmond.api.service.graphrag.CareerPath
import com.goalmond.api.service.graphrag.GraphSearchService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.math.roundToInt
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
    private val graphSearchService: GraphSearchService,
    private val graphRagEntityRepository: GraphRagEntityRepository,
    private val hardFilterService: HardFilterService,
    private val scoringService: ScoringService,
    private val pathOptimizationService: PathOptimizationService,
    private val riskPenaltyService: RiskPenaltyService,
    private val explanationService: ExplanationService,
    private val fallbackMatchingService: FallbackMatchingService,
    private val hybridRankingService: HybridRankingService
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
            val vectorCandidates = try {
                vectorSearchService.searchSimilarSchools(user, profile, preference)
                    .also { logger.info("벡터 검색 완료: ${it.size}개 후보군") }
            } catch (e: VectorSearchException) {
                logger.warn("벡터 검색 실패 → Fallback 전환 (user=$userId): ${e.message}")
                emptyList()
            }

            if (vectorCandidates.isEmpty()) {
                logger.info("후보군 없음 → Fallback(AI 추천) 실행")
                val fallbackResults = fallbackMatchingService.generateFallbackResults(profile, preference)
                return buildFallbackMatchingResponse(userId, preference, fallbackResults, startTime)
            }

            val vectorSchoolMap = vectorCandidates.mapNotNull { candidate ->
                val schoolId = candidate.school.id ?: return@mapNotNull null
                schoolId to candidate.school
            }.toMap()

            val vectorScoreMap = vectorCandidates.mapNotNull { candidate ->
                val schoolId = candidate.school.id ?: return@mapNotNull null
                schoolId to candidate.similarity
            }.toMap()

            if (vectorSchoolMap.isEmpty()) {
                logger.warn("벡터 후보 중 유효한 학교 ID 없음 → Fallback(AI 추천)")
                val fallbackResults = fallbackMatchingService.generateFallbackResults(profile, preference)
                return buildFallbackMatchingResponse(userId, preference, fallbackResults, startTime)
            }

            val graphIntent = buildGraphIntent(preference)
            val careerPaths = graphIntent?.let { intent ->
                graphSearchService.findCareerPaths(
                    targetCompany = intent.companyName,
                    targetJob = intent.jobName,
                    requiredSkills = emptyList(),
                    maxDepth = 4
                )
            } ?: emptyList()

            val detectedSkills = detectSkillsFromPreference(preference)
            val skillProgramMatches = if (detectedSkills.isNotEmpty()) {
                graphSearchService.findProgramsBySkills(detectedSkills, topN = 30)
            } else {
                emptyList()
            }

            logger.info(
                "Hybrid graph signals: careerPaths={}, detectedSkills={}, skillProgramMatches={}",
                careerPaths.size,
                detectedSkills,
                skillProgramMatches.size
            )
            
            // Step 3: School에 연결된 Program 조회
            val schoolIds = vectorSchoolMap.keys.toList()
            val programs = programRepository.findBySchoolIdIn(schoolIds)
            logger.info("프로그램 조회: ${programs.size}개")
            
            // Step 4: Hard Filter
            val filterResult = hardFilterService.filterPrograms(profile, preference, programs)
            logger.info("Hard Filter: ${filterResult.passedCount()}개 통과, ${filterResult.filteredCount()}개 필터링")
            
            // Fallback: Hard Filter 통과 결과가 없으면 AI 추천 생성 (하이브리드 방식)
            if (filterResult.passed.isEmpty()) {
                logger.info("Hard Filter 통과 결과 없음 → Fallback(AI 추천) 실행")

                val budgetFiltered = filterResult.filtered.count { it.filterType == com.goalmond.api.domain.dto.FilterType.BUDGET_EXCEEDED }
                val englishFiltered = filterResult.filtered.count { it.filterType == com.goalmond.api.domain.dto.FilterType.ENGLISH_SCORE }
                val visaFiltered = filterResult.filtered.count { it.filterType == com.goalmond.api.domain.dto.FilterType.VISA_REQUIREMENT }

                val minTuition = programs.mapNotNull { program ->
                    program.tuition ?: vectorSchoolMap[program.schoolId]?.tuition
                }.minOrNull()

                val fallbackResults = fallbackMatchingService.generateFallbackResults(profile, preference)
                return buildFallbackMatchingResponse(userId, preference, fallbackResults, startTime).copy(
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
                val school = vectorSchoolMap[program.schoolId] ?: return@mapNotNull null

                val scores = scoringService.calculateScore(profile, preference, program, school)
                val optimization = pathOptimizationService.applyOptimization(profile, preference, program, school)
                val penalty = riskPenaltyService.applyPenalty(profile, preference, program, school)
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

            val top5 = hybridRankingService.rankCandidates(
                candidates = candidates,
                vectorScoreMap = vectorScoreMap,
                careerPaths = careerPaths,
                skillProgramMatches = skillProgramMatches,
                detectedSkills = detectedSkills
            )

            logger.info("Top 5 선정 완료: Hybrid 점수 ${top5.firstOrNull()?.rankingScore} ~ ${top5.lastOrNull()?.rankingScore}")

            val results = top5.mapIndexed { index, candidate ->
                val (pros, cons) = explanationService.generateProsAndCons(
                    profile, preference, candidate.program, candidate.school, candidate.scores
                )

                val recommendationType = classifyRecommendationType(candidate.rankingScore)
                val explanation = generateSimpleExplanation(candidate, recommendationType)

                MatchingResponse.MatchingResult(
                    rank = index + 1,
                    school = buildSchoolSummary(candidate.school),
                    program = buildProgramSummary(candidate.program),
                    totalScore = candidate.rankingScore,
                    estimatedRoi = calculateEstimatedRoi(candidate.school, candidate.program),
                    scoreBreakdown = buildScoreBreakdown(candidate.scores),
                    indicatorScores = buildIndicatorScores(candidate.scores),
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
                createdAt = Instant.now(),
                indicatorDescription = generateIndicatorDescription(results.firstOrNull()),
                nextSteps = generateNextSteps(preference)
            )
            
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startTime
            logger.error("매칭 실패 (${elapsed}ms)", e)
            throw MatchingException("Matching failed for user $userId", e)
        }
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
        val dataHints = mutableListOf<String>()
        school.employmentRate?.let { dataHints.add("취업률 ${it}%") }
        school.eslProgram?.takeIf { it.contains("true", ignoreCase = true) }?.let { dataHints.add("ESL 프로그램 제공") }
        school.facilities?.takeIf { it.contains("dormitory", ignoreCase = true) }?.let { dataHints.add("기숙사 정보 확인 가능") }
        val hintText = if (dataHints.isNotEmpty()) " 또한 ${dataHints.joinToString(", ")} 기준으로 실무 준비도를 확인했습니다." else ""

        val graphNarrative = candidate.graphPath?.let { path ->
            val skillsText = path.skills.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "관련 역량"
            val programName = path.programName ?: candidate.program.name
            val jobName = path.job ?: "목표 직무"
            " ${path.company}은 ${path.schoolName}의 ${programName} 졸업생을 ${jobName}으로 채용하며 ${skillsText}을(를) 강조합니다."
        } ?: ""

        val baseText = when (type) {
            "safe" -> "이 학교는 예산 대비 학비가 안정적이며, 귀하의 영어 점수로 바로 입학이 가능하고, 졸업 후 OPT 연계 확률이 높아 추천되었습니다."
            "challenge" -> "이 학교는 ${school.city}에 위치하며 편입률이 높아 장기적으로 유리하지만, 경쟁률을 고려하여 도전해볼 만한 선택지입니다."
            else -> "이 학교는 예산과 목표에 맞춰 전략적으로 선택할 수 있는 프로그램입니다."
        }

        return "$baseText$hintText$graphNarrative"
    }

    /**
     * 상위 추천 결과의 통합 지표를 기반으로 설명 문구를 만든다.
     */
    private fun generateIndicatorDescription(topResult: MatchingResponse.MatchingResult?): String? {
        if (topResult == null) return null
        val scores = topResult.indicatorScores
        val strengths = mutableListOf<String>()
        if (scores.academicFit >= 80) strengths.add("학업 적합도")
        if (scores.careerOutlook >= 80) strengths.add("진로 전망")
        if (scores.costEfficiency >= 80) strengths.add("비용 효율성")

        return when {
            strengths.size >= 2 -> "${strengths.joinToString("과 ")}에서 가장 높은 적합성을 보입니다."
            strengths.size == 1 -> "${strengths[0]}가 특히 우수합니다."
            else -> "전반적으로 균형 잡힌 매칭 결과입니다."
        }
    }

    /**
     * 사용자 선호 정보를 기반으로 다음 단계 가이드를 생성한다.
     */
    private fun generateNextSteps(preference: UserPreference): List<MatchingResponse.NextStep> {
        val regionHint = preference.targetLocation?.takeIf { it.isNotBlank() } ?: "희망 지역"
        return listOf(
            MatchingResponse.NextStep(
                id = 1,
                title = "지원 서류 점검",
                description = "성적표, 자기소개서, 추천서 초안을 먼저 준비하세요.",
                priority = "recommended"
            ),
            MatchingResponse.NextStep(
                id = 2,
                title = "학교별 일정 확인",
                description = "$regionHint 중심으로 상위 추천 학교의 원서 마감일을 확인하세요.",
                priority = "recommended"
            ),
            MatchingResponse.NextStep(
                id = 3,
                title = "영어/인터뷰 준비",
                description = "영어 점수 제출 또는 인터뷰 대비 자료를 준비하세요.",
                priority = "recommended"
            ),
            MatchingResponse.NextStep(
                id = 4,
                title = "비자 및 재정 계획",
                description = "예상 학비/생활비 기준으로 재정 증빙 및 비자 준비를 시작하세요.",
                priority = "optional"
            )
        )
    }

    private fun buildFallbackMatchingResponse(
        userId: UUID,
        preference: UserPreference,
        fallbackResults: List<MatchingResponse.MatchingResult>,
        startTime: Long
    ): MatchingResponse {
        val elapsed = System.currentTimeMillis() - startTime
        return MatchingResponse(
            matchingId = UUID.randomUUID().toString(),
            userId = userId.toString(),
            totalMatches = fallbackResults.size,
            executionTimeMs = elapsed.toInt(),
            results = fallbackResults,
            createdAt = Instant.now(),
            message = "맞춤형 추천을 제공합니다.",
            indicatorDescription = generateIndicatorDescription(fallbackResults.firstOrNull()),
            nextSteps = generateNextSteps(preference)
        )
    }

    private fun selectGraphPath(candidate: MatchingCandidate, paths: List<CareerPath>): CareerPath? {
        val schoolId = candidate.school.id ?: return null
        val matchingPaths = paths.filter { it.schoolId == schoolId }
        if (matchingPaths.isEmpty()) return null
        val programMatch = candidate.program.id?.let { pid ->
            matchingPaths.firstOrNull { it.programId == pid }
        }
        return programMatch ?: matchingPaths.maxByOrNull { it.weight }
    }

    private fun computeGraphScore(path: CareerPath?, maxWeight: Double): Double {
        if (path == null || maxWeight <= 0.0) return 0.0
        return (path.weight / maxWeight).coerceIn(0.0, 1.0)
    }

    private fun buildGraphIntent(preference: UserPreference): GraphIntent? {
        val goal = preference.careerGoal?.trim().takeUnless { it.isNullOrBlank() } ?: return null
        val companyName = detectCompanyFromGoal(goal) ?: return null
        val jobName = detectJobFromGoal(goal)
        return GraphIntent(companyName, jobName)
    }

    private fun detectCompanyFromGoal(goal: String): String? {
        detectEntityName(EntityType.COMPANY, goal)?.let { return it }
        val keywords = listOf(" at ", " @ ", " for ")
        keywords.forEach { keyword ->
            val candidate = extractAfterKeyword(goal, keyword)
            if (!candidate.isNullOrBlank()) {
                detectEntityName(EntityType.COMPANY, candidate)?.let { return it }
                return candidate
            }
        }
        return null
    }

    private fun detectJobFromGoal(goal: String): String? {
        detectEntityName(EntityType.JOB, goal)?.let { return it }
        val beforeAt = goal.substringBeforeLast(" at ", goal).trim()
        if (beforeAt.isBlank() || beforeAt == goal) return null
        detectEntityName(EntityType.JOB, beforeAt)?.let { return it }
        return beforeAt
    }

    private fun extractAfterKeyword(text: String, keyword: String): String? {
        val lower = text.lowercase()
        val index = lower.lastIndexOf(keyword.lowercase())
        if (index < 0) return null
        val start = index + keyword.length
        val fragment = text.substring(start).split(Regex("[,\\.]"))[0].trim()
        return fragment.takeIf { it.isNotBlank() }
    }

    private fun detectEntityName(entityType: EntityType, term: String): String? {
        val trimmed = term.trim()
        if (trimmed.isBlank()) return null
        return graphRagEntityRepository.searchByTypeAndName(entityType, trimmed)
            .firstOrNull()?.entityName
    }

    private fun detectSkillsFromPreference(preference: UserPreference): List<String> {
        val sourceTerms = listOfNotNull(preference.targetMajor, preference.careerGoal)
            .flatMap { extractTerms(it) }
            .distinct()

        if (sourceTerms.isEmpty()) return emptyList()

        return sourceTerms.mapNotNull { term ->
            detectEntityName(EntityType.SKILL, term)
        }.distinct().take(MAX_DETECTED_SKILLS)
    }

    private fun extractTerms(text: String): List<String> {
        return text.lowercase()
            .split(Regex("[^a-z0-9+#]+"))
            .map { it.trim() }
            .filter {
                it.length >= 2 &&
                    it !in STOP_WORDS
            }
    }

    private data class GraphIntent(
        val companyName: String,
        val jobName: String?
    )
    
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
            featureBadges = parseFeatureBadges(school.featureBadges),
            employmentRate = school.employmentRate?.toDouble(),
            facilities = parseFacilities(school.facilities),
            staffInfo = school.staffInfo,
            eslProgram = parseEslProgram(school.eslProgram),
            internationalSupport = parseInternationalSupport(school.internationalSupport),
            internationalEmail = school.internationalEmail,
            internationalPhone = school.internationalPhone
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

    private fun parseFacilities(raw: String?): MatchingResponse.FacilityInfo? {
        if (raw.isNullOrBlank()) return null
        return try {
            jacksonObjectMapper().readValue<MatchingResponse.FacilityInfo>(raw)
        } catch (e: Exception) {
            logger.debug("facilities 파싱 실패: {}", raw, e)
            null
        }
    }

    private fun parseEslProgram(raw: String?): MatchingResponse.EslProgramInfo? {
        if (raw.isNullOrBlank()) return null
        return try {
            jacksonObjectMapper().readValue<MatchingResponse.EslProgramInfo>(raw)
        } catch (e: Exception) {
            logger.debug("esl_program 파싱 실패: {}", raw, e)
            null
        }
    }

    private fun parseInternationalSupport(raw: String?): MatchingResponse.InternationalSupportInfo? {
        if (raw.isNullOrBlank()) return null
        return try {
            jacksonObjectMapper().readValue<MatchingResponse.InternationalSupportInfo>(raw)
        } catch (e: Exception) {
            logger.debug("international_support 파싱 실패: {}", raw, e)
            null
        }
    }

    private companion object {
        const val MAX_DETECTED_SKILLS = 5
        val STOP_WORDS = setOf(
            "and", "or", "the", "for", "with", "to", "in", "at", "of", "on", "a", "an",
            "취업", "목표", "희망", "전공", "진로", "분야", "관련"
        )
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

    /**
     * 프론트엔드 선형 게이지용 통합 지표 계산.
     * score_breakdown 조합: academicFit=(academic+english)/2, careerOutlook=(career+location)/2, costEfficiency=(budget+duration)/2.
     */
    private fun buildIndicatorScores(scores: ScoreBreakdown): MatchingResponse.IndicatorScores {
        return MatchingResponse.IndicatorScores(
            academicFit = ((scores.academic + scores.english) / 2.0).roundToInt(),
            careerOutlook = ((scores.career + scores.location) / 2.0).roundToInt(),
            costEfficiency = ((scores.budget + scores.duration) / 2.0).roundToInt()
        )
    }
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
    val totalScore: Double,
    val vectorScore: Double = 0.0,
    val graphScore: Double = 0.0,
    val graphPath: CareerPath? = null,
    val rankingScore: Double = 0.0
)

/**
 * 매칭 예외.
 */
class MatchingException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
