package com.goalmond.api.service.matching

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.goalmond.api.domain.dto.MatchingResponse
import com.goalmond.api.domain.entity.AcademicProfile
import com.goalmond.api.domain.entity.UserPreference
import com.goalmond.api.service.ai.GeminiClient
import com.goalmond.api.service.ai.GeminiApiException
import kotlin.math.roundToInt
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * DB에 학교/임베딩 데이터가 없을 때, 프로필·선호도만으로 Gemini를 호출해 추천 결과를 생성하는 Fallback 서비스.
 * 
 * 벡터 검색 후보가 0건일 때만 사용하며, 동일한 MatchingResponse 형식으로 반환한다.
 * 응답 message에 "DB에 데이터가 없어 API 정보만으로 생성한 추천" 안내를 담는다.
 * 
 * 개선 사항 (GAM-3 Phase 9):
 * - 프롬프트 엔지니어링: 매칭 기준 6대 지표, 추천 유형, 확장 필드 명시
 * - 에러 처리 강화: Gemini API 실패 시 기본 추천 제공
 * - 부분 성공 처리: 5개 중 일부만 파싱 성공해도 반환
 * - 확장 필드 지원: globalRanking, averageSalary 등 매칭 리포트 필드
 */
@Service
class FallbackMatchingService(
    private val geminiClient: GeminiClient,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val MAX_RECOMMENDATIONS = 5
        private const val DEFAULT_TOTAL_SCORE = 70.0
        private const val DEFAULT_BREAKDOWN_SCORE = 75
        
        // 추천 유형
        private val VALID_RECOMMENDATION_TYPES = setOf("safe", "challenge", "strategy")
    }

    private val defaultScoreBreakdown = MatchingResponse.ScoreBreakdown(
        academic = DEFAULT_BREAKDOWN_SCORE,
        english = DEFAULT_BREAKDOWN_SCORE,
        budget = DEFAULT_BREAKDOWN_SCORE,
        location = DEFAULT_BREAKDOWN_SCORE,
        duration = DEFAULT_BREAKDOWN_SCORE,
        career = DEFAULT_BREAKDOWN_SCORE
    )

    /**
     * 프로필·선호도 기반으로 Gemini가 생성한 추천 5건을 MatchingResponse.MatchingResult 목록으로 반환.
     * 
     * 개선된 에러 처리:
     * - Gemini API 실패 → 기본 추천 반환
     * - JSON 파싱 실패 → 기본 추천 반환
     * - 파싱 결과 비어있음 → 기본 추천 반환
     * 
     * @param profile 학력 프로필
     * @param preference 유학 선호도
     * @return 추천 결과 목록 (항상 1개 이상 반환 보장)
     */
    fun generateFallbackResults(
        profile: AcademicProfile,
        preference: UserPreference
    ): List<MatchingResponse.MatchingResult> {
        val prompt = buildPrompt(profile, preference)
        logger.info("Fallback 매칭 시작: major=${preference.targetMajor}, budget=${preference.budgetUsd}, location=${preference.targetLocation}")
        logger.debug("Gemini 프롬프트:\n$prompt")
        
        return try {
            val raw = geminiClient.generateContent(prompt)
            logger.info("Gemini API 호출 성공 (응답 길이: ${raw.length}자)")
            logger.debug("Gemini 응답:\n${raw.take(1000)}...")
            
            val results = parseAndMap(raw)
            if (results.isEmpty()) {
                logger.warn("Gemini 응답 파싱 결과가 비어있음, 기본 추천 사용")
                generateDefaultRecommendations(profile, preference)
            } else {
                logger.info("Fallback 매칭 성공: ${results.size}개 추천 생성")
                results
            }
        } catch (e: GeminiApiException) {
            logger.error("Gemini API 호출 실패: ${e.message} (cause: ${e.cause?.message}), 기본 추천 사용", e)
            generateDefaultRecommendations(profile, preference)
        } catch (e: Exception) {
            logger.error("Fallback 매칭 실패: ${e.message}, 기본 추천 사용", e)
            generateDefaultRecommendations(profile, preference)
        }
    }

    /**
     * 개선된 프롬프트 생성.
     * 
     * 포함 요소:
     * - 역할 정의: "유학 매칭 전문가"
     * - 매칭 기준: 6대 지표(academic 20%, english 15%, budget 15%, location 10%, duration 10%, career 30%)
     * - 추천 유형: safe/challenge/strategy 정의
     * - 출력 형식: 모든 필드 타입과 예시 명시
     * - 제약 조건: 예산 초과 금지, 실제 학교만, 품질 요구사항
     */
    internal fun buildPrompt(profile: AcademicProfile, preference: UserPreference): String {
        val gpa = profile.gpa ?: "N/A"
        val gpaScale = profile.gpaScale ?: "4.0"
        val englishScore = profile.englishScore?.let { "$it (${profile.englishTestType ?: "N/A"})" } ?: "미제출"
        val targetMajor = preference.targetMajor ?: "미정"
        val targetProgram = preference.targetProgram ?: "community_college"
        val targetLocation = preference.targetLocation ?: "미정"
        val budgetUsd = preference.budgetUsd ?: 30000
        val careerGoal = preference.careerGoal ?: "미정"
        val preferredTrack = preference.preferredTrack ?: "편입"
        
        return """
당신은 미국 유학 매칭 전문가입니다. 다음 학생에게 최적의 학교와 프로그램을 추천해주세요.

**중요: 아래 조건을 반드시 준수하세요:**
1. 연간 예산 $${budgetUsd} USD를 절대 초과하지 않는 학교만 추천하세요.
2. 희망 지역 "$targetLocation"의 학교를 우선 추천하세요.
3. 희망 전공 "$targetMajor"와 관련된 프로그램만 추천하세요.
4. 5개 학교는 모두 달라야 하며, 다양한 선택지를 제공하세요.

[학생 프로필]
- 학교: ${profile.schoolName ?: "N/A"}
- 학위: ${profile.degree ?: "N/A"}
- GPA: $gpa / $gpaScale
- 영어 점수: $englishScore

[선호도 및 목표]
- 희망 전공: $targetMajor
- 희망 프로그램 유형: $targetProgram
- 희망 지역: $targetLocation
- 연간 예산: $${budgetUsd} USD
- 커리어 목표: $careerGoal
- 선호 트랙: $preferredTrack

[매칭 기준] 다음 6가지 지표를 고려하여 추천하세요:
1. 학업 적합도 (20%): GPA와 학교의 입학 요건 비교
2. 영어 적합도 (15%): 영어 점수와 학교의 영어 요건 비교
3. 예산 적합도 (15%): 학비+생활비가 예산 내인지 (예산 초과 학교는 제외)
4. 지역 선호 (10%): 희망 지역과의 일치도
5. 기간 적합도 (10%): 프로그램 기간과 목표의 일치도
6. 진로 연계성 (30%): 커리어 목표와 프로그램/학교의 연관성

[추천 유형 정의]
- "safe": 합격 가능성 높음 (예산 적합, 입학 요건 충족, 합격률 높음)
- "challenge": 도전적 선택 (명문대, 경쟁률 높음, 높은 성과 필요)
- "strategy": 전략적 선택 (편입 경로, 비용 효율, 장기적 이점)

[필수 출력 형식] 반드시 아래 JSON 배열만 출력하세요. 다른 설명이나 마크다운 없이 JSON만 출력합니다.
**중요: 모든 텍스트 필드(explanation, pros, cons, program_name 등)는 반드시 한국어로 작성하세요.**
[
  {
    "school_name": "학교 정식 명칭 (예: Santa Monica College)",
    "school_type": "community_college 또는 university 또는 vocational",
    "state": "주 약어 (예: CA, NY, TX)",
    "city": "도시명 (예: Santa Monica)",
    "tuition": 연간학비_숫자만 (예: 12000),
    "global_ranking": "글로벌 랭킹 문자열 또는 null (예: #4 또는 null)",
    "ranking_field": "랭킹 기준 전공 또는 null (예: Computer Science 또는 null)",
    "average_salary": 평균초봉_숫자_또는_null (예: 85000 또는 null),
    "alumni_network_count": 동문수_숫자_또는_null (예: 38000 또는 null),
    "feature_badges": ["특징 배지 한글로"] (예: ["높은 편입률", "저렴한 학비", "OPT STEM 인정"]),
    "program_name": "프로그램명 한글로 (예: 컴퓨터공학 편입 프로그램)",
    "degree": "학위 (예: AA, AS, BS, MS)",
    "duration": "기간 (예: 2년, 4년)",
    "opt_available": true_또는_false,
    "recommendation_type": "safe 또는 challenge 또는 strategy",
    "total_score": 총점_0~100 (예: 85),
    "score_breakdown": {
      "academic": 학업점수_0~100,
      "english": 영어점수_0~100,
      "budget": 예산점수_0~100,
      "location": 지역점수_0~100,
      "duration": 기간점수_0~100,
      "career": 진로점수_0~100
    },
    "explanation": "이 학교를 추천하는 구체적 이유 2-3문장. 매칭 기준(6대 지표)을 명시적으로 언급하세요.",
    "pros": ["구체적 장점1", "구체적 장점2", "구체적 장점3"],
    "cons": ["고려할 단점1", "고려할 단점2"]
  }
]

[중요 지침]
1. 정확히 5개 추천을 생성하세요
2. 모든 숫자 필드는 숫자만 입력 (달러 기호, 쉼표, 따옴표 없이)
3. pros는 최소 3개, cons는 최소 2개 작성
4. explanation은 학생의 프로필과 매칭 기준을 명시적으로 언급
5. 실제 존재하는 미국 학교만 추천
6. 연간 예산($$budgetUsd)을 초과하는 학교는 추천하지 않음
7. recommendation_type은 반드시 "safe", "challenge", "strategy" 중 하나
8. 첫 번째 추천은 "safe", 두 번째는 "challenge", 나머지는 다양하게 배치
9. 모든 explanation, pros, cons는 한국어로 작성 (영어 사용 금지)
10. 학교명(school_name)은 원어(영어) 그대로 유지 (예: Santa Monica College)
        """.trimIndent()
    }

    /**
     * Gemini 응답을 파싱하여 MatchingResult 목록으로 변환.
     * 
     * 개선 사항:
     * - 부분 성공 처리: 개별 항목 파싱 실패해도 성공한 항목만 반환
     * - 확장 필드 파싱: globalRanking, averageSalary 등
     * - null 안전 처리 강화
     */
    internal fun parseAndMap(raw: String): List<MatchingResponse.MatchingResult> {
        val jsonStr = extractJsonArray(raw)
        if (jsonStr == null) {
            logger.warn("JSON 배열 추출 실패. 원본 응답 (첫 200자): ${raw.take(200)}")
            return emptyList()
        }
        
        val root = try {
            objectMapper.readTree(jsonStr)
        } catch (e: Exception) {
            logger.warn("JSON 파싱 실패: ${e.message}. 추출된 JSON (첫 200자): ${jsonStr.take(200)}")
            return emptyList()
        }
        
        if (!root.isArray) {
            logger.warn("응답이 JSON 배열이 아님: ${root.nodeType}")
            return emptyList()
        }
        
        // 부분 성공 처리: 개별 항목을 try-catch로 감싸서 일부 실패해도 나머지 반환
        val results = mutableListOf<MatchingResponse.MatchingResult>()
        root.take(MAX_RECOMMENDATIONS).forEachIndexed { index, node ->
            try {
                results.add(parseNode(node, index))
            } catch (e: Exception) {
                logger.warn("결과 $index 파싱 실패: ${e.message}")
            }
        }
        
        return results
    }

    /**
     * 개별 JSON 노드를 MatchingResult로 변환.
     * 
     * 확장 필드 지원:
     * - globalRanking, rankingField, averageSalary, alumniNetworkCount, featureBadges
     * - recommendationType (AI가 직접 분류)
     * - scoreBreakdown (AI가 계산한 점수)
     */
    private fun parseNode(node: JsonNode, index: Int): MatchingResponse.MatchingResult {
        val rank = index + 1
        
        // 기본 학교 정보
        val schoolName = node.path("school_name").asText("추천 학교 $rank")
        val schoolType = node.path("school_type").asText("community_college")
        val state = node.path("state").asText("")
        val city = node.path("city").asText("")
        val tuition = node.path("tuition").asInt(0)
        
        // 확장 필드 (null 허용)
        val globalRanking = node.path("global_ranking").takeIf { !it.isNull && !it.isMissingNode }?.asText()
        val rankingField = node.path("ranking_field").takeIf { !it.isNull && !it.isMissingNode }?.asText()
        val averageSalary = node.path("average_salary").takeIf { !it.isNull && !it.isMissingNode }?.asInt()
        val alumniNetworkCount = node.path("alumni_network_count").takeIf { !it.isNull && !it.isMissingNode }?.asInt()
        val featureBadges = node.path("feature_badges")
            .takeIf { it.isArray }
            ?.mapNotNull { it.asText()?.takeIf { s -> s.isNotBlank() } }
            ?: emptyList()
        
        // 프로그램 정보
        val programName = node.path("program_name").asText("편입 프로그램")
        val degree = node.path("degree").asText("Associate")
        val duration = node.path("duration").asText("2년")
        val optAvailable = node.path("opt_available").asBoolean(true)
        
        // 추천 유형 (AI가 직접 분류)
        val rawRecommendationType = node.path("recommendation_type").asText("strategy")
        val recommendationType = if (rawRecommendationType in VALID_RECOMMENDATION_TYPES) {
            rawRecommendationType
        } else {
            "strategy"
        }
        
        // 점수 (AI가 계산한 값 사용, 없으면 기본값)
        val totalScore = node.path("total_score").asDouble(DEFAULT_TOTAL_SCORE)
        val scoreBreakdown = parseScoreBreakdown(node.path("score_breakdown"))
        
        // 설명 및 장단점
        val explanation = node.path("explanation").asText("프로필에 맞춘 AI 추천입니다.")
        val pros = node.path("pros")
            .takeIf { it.isArray }
            ?.mapNotNull { it.asText()?.takeIf { s -> s.isNotBlank() } }
            ?.take(5)
            ?: listOf("AI 기반 맞춤 추천", "예산 적합", "커리어 목표 부합")
        val cons = node.path("cons")
            .takeIf { it.isArray }
            ?.mapNotNull { it.asText()?.takeIf { s -> s.isNotBlank() } }
            ?.take(3)
            ?: listOf("추가 정보 확인 필요")
        
        return MatchingResponse.MatchingResult(
            rank = rank,
            school = MatchingResponse.SchoolSummary(
                id = "fallback-$rank",
                name = schoolName,
                type = schoolType,
                state = state,
                city = city,
                tuition = tuition,
                imageUrl = "",
                globalRanking = globalRanking,
                rankingField = rankingField,
                averageSalary = averageSalary,
                alumniNetworkCount = alumniNetworkCount,
                featureBadges = featureBadges
            ),
            program = MatchingResponse.ProgramSummary(
                id = "fallback-$rank-p",
                name = programName,
                degree = degree,
                duration = duration,
                optAvailable = optAvailable
            ),
            totalScore = totalScore.coerceIn(0.0, 100.0),
            estimatedRoi = 0.0,
            scoreBreakdown = scoreBreakdown,
            indicatorScores = calculateIndicatorScores(scoreBreakdown),
            recommendationType = recommendationType,
            explanation = explanation,
            pros = pros,
            cons = cons
        )
    }

    /**
     * 프론트엔드 선형 게이지용 통합 지표 계산.
     * score_breakdown 조합: academicFit=(academic+english)/2, careerOutlook=(career+location)/2, costEfficiency=(budget+duration)/2.
     */
    private fun calculateIndicatorScores(breakdown: MatchingResponse.ScoreBreakdown): MatchingResponse.IndicatorScores {
        return MatchingResponse.IndicatorScores(
            academicFit = ((breakdown.academic + breakdown.english) / 2.0).roundToInt(),
            careerOutlook = ((breakdown.career + breakdown.location) / 2.0).roundToInt(),
            costEfficiency = ((breakdown.budget + breakdown.duration) / 2.0).roundToInt()
        )
    }
    
    /**
     * 점수 breakdown 파싱. 없으면 기본값 사용.
     */
    private fun parseScoreBreakdown(node: JsonNode): MatchingResponse.ScoreBreakdown {
        if (node.isNull || node.isMissingNode) {
            return defaultScoreBreakdown
        }
        return MatchingResponse.ScoreBreakdown(
            academic = node.path("academic").asInt(DEFAULT_BREAKDOWN_SCORE).coerceIn(0, 100),
            english = node.path("english").asInt(DEFAULT_BREAKDOWN_SCORE).coerceIn(0, 100),
            budget = node.path("budget").asInt(DEFAULT_BREAKDOWN_SCORE).coerceIn(0, 100),
            location = node.path("location").asInt(DEFAULT_BREAKDOWN_SCORE).coerceIn(0, 100),
            duration = node.path("duration").asInt(DEFAULT_BREAKDOWN_SCORE).coerceIn(0, 100),
            career = node.path("career").asInt(DEFAULT_BREAKDOWN_SCORE).coerceIn(0, 100)
        )
    }

    /**
     * 응답 텍스트에서 JSON 배열 부분만 추출.
     * 
     * 처리:
     * - 마크다운 코드블록 제거 (```json ... ```)
     * - 앞뒤 설명 텍스트 제거
     */
    private fun extractJsonArray(raw: String): String? {
        var s = raw.trim()
        
        // 마크다운 코드블록 제거
        if (s.contains("```")) {
            s = s.replace(Regex("```json\\s*"), "")
                .replace(Regex("```\\s*"), "")
                .trim()
        }
        
        val start = s.indexOf('[')
        val end = s.lastIndexOf(']')
        if (start < 0 || end <= start) {
            return null
        }
        return s.substring(start, end + 1)
    }
    
    /**
     * Gemini API 실패 또는 파싱 실패 시 사용하는 기본 추천 생성.
     *
     * 사용자 입력(예산, 지역, 프로그램 유형) 기반 필터링 및 정렬 후 5개 반환.
     */
    internal fun generateDefaultRecommendations(
        profile: AcademicProfile,
        preference: UserPreference
    ): List<MatchingResponse.MatchingResult> {
        val targetMajor = preference.targetMajor ?: "일반"
        val budget = preference.budgetUsd ?: 30000
        val targetLocation = preference.targetLocation?.trim() ?: ""
        val targetProgram = preference.targetProgram ?: "community_college"
        logger.info("기본 추천 생성: major=$targetMajor, budget=$budget, location=$targetLocation, programType=$targetProgram")
        
        // 1. 예산 필터링 (학비가 예산의 70% 이하, 생활비 고려)
        var filtered = getAllDefaultTemplates(targetMajor).filter { it.tuition <= budget * 0.7 }
        if (filtered.isEmpty()) filtered = getAllDefaultTemplates(targetMajor)
        
        // 2. 프로그램 유형 필터링
        val byType = filtered.filter { it.type == targetProgram }
        if (byType.isNotEmpty()) filtered = byType
        
        // 3. 지역 우선순위 정렬 (지역 일치 시 상단)
        val locationUpper = targetLocation.uppercase()
        val prioritized = filtered.sortedByDescending { template ->
            var score = 0.0
            if (locationUpper.isNotBlank()) {
                if (template.state.uppercase().contains(locationUpper) ||
                    template.city.uppercase().contains(locationUpper) ||
                    locationUpper.contains(template.state.uppercase()) ||
                    "CALIFORNIA".contains(locationUpper) && template.state == "CA" ||
                    "NEW YORK".contains(locationUpper) && template.state == "NY" ||
                    "TEXAS".contains(locationUpper) && template.state == "TX") {
                    score += 100.0
                }
            }
            score += (budget - template.tuition) / 1000.0.coerceAtLeast(1.0)
            score
        }
        
        val selected = prioritized.distinctBy { it.name }.take(MAX_RECOMMENDATIONS)
        val programNamesByMajor = getProgramNamesForMajor(targetMajor)
        
        return selected.mapIndexed { index, template ->
            val rank = index + 1
            val programName = programNamesByMajor.getOrElse(index) { "$targetMajor 전공 과정" }
            MatchingResponse.MatchingResult(
                rank = rank,
                school = MatchingResponse.SchoolSummary(
                    id = "fallback-$rank",
                    name = template.name,
                    type = template.type,
                    state = template.state,
                    city = template.city,
                    tuition = template.tuition,
                    imageUrl = "",
                    globalRanking = null,
                    rankingField = null,
                    averageSalary = null,
                    alumniNetworkCount = null,
                    featureBadges = template.featureBadges
                ),
                program = MatchingResponse.ProgramSummary(
                    id = "fallback-$rank-p",
                    name = programName,
                    degree = template.degree,
                    duration = template.duration,
                    optAvailable = true
                ),
                totalScore = template.totalScore,
                estimatedRoi = 0.0,
                scoreBreakdown = defaultScoreBreakdown,
                indicatorScores = calculateIndicatorScores(defaultScoreBreakdown),
                recommendationType = template.recommendationType,
                explanation = template.explanation,
                pros = template.pros,
                cons = template.cons
            )
        }
    }
    
    /**
     * 전공에 따른 프로그램명 목록 (다양화).
     */
    private fun getProgramNamesForMajor(targetMajor: String): List<String> {
        val lower = targetMajor.lowercase()
        return when {
            lower.contains("컴퓨터") || lower.contains("소프트웨어") || lower.contains("computer") || lower.contains("software") ->
                listOf("컴퓨터공학 편입 프로그램", "소프트웨어 개발 집중 과정", "컴퓨터 과학 Associate Degree", "IT 및 컴퓨터 응용", "CS 편입 트랙")
            lower.contains("경영") || lower.contains("비즈니스") || lower.contains("business") ->
                listOf("경영학 편입 프로그램", "비즈니스 관리 과정", "경영 및 회계 Associate", "경제학 편입", "MBA 준비 과정")
            lower.contains("기계") || lower.contains("공학") || lower.contains("engineering") ->
                listOf("기계공학 전공 과정", "엔지니어링 편입 프로그램", "STEM Associate", "산업 기술 과정", "공학 기초 과정")
            else -> listOf("$targetMajor 편입 프로그램", "$targetMajor 전공 과정", "$targetMajor 집중 과정", "$targetMajor Associate", "$targetMajor 과정")
        }
    }
    
    /**
     * 기본 추천용 전체 템플릿 (지역·유형별 다양화).
     */
    private fun getAllDefaultTemplates(targetMajor: String): List<DefaultSchoolTemplate> {
        val baseProgramName = "$targetMajor 편입 프로그램"
        return listOf(
            DefaultSchoolTemplate(
                name = "Santa Monica College",
                type = "community_college",
                state = "CA",
                city = "Santa Monica",
                tuition = 9000,
                programName = baseProgramName,
                degree = "AA",
                duration = "2년",
                recommendationType = "safe",
                explanation = "예산 내에서 높은 편입률을 자랑하는 커뮤니티 칼리지입니다. UCLA, USC 등 명문대 편입 실적이 우수합니다.",
                pros = listOf("높은 UC 편입률", "저렴한 학비", "다양한 전공 제공", "LA 위치"),
                cons = listOf("경쟁이 치열할 수 있음", "기숙사 미제공"),
                featureBadges = listOf("높은 편입률", "저렴한 학비"),
                totalScore = 85.0
            ),
            DefaultSchoolTemplate(
                name = "De Anza College",
                type = "community_college",
                state = "CA",
                city = "Cupertino",
                tuition = 9500,
                programName = baseProgramName,
                degree = "AS",
                duration = "2년",
                recommendationType = "challenge",
                explanation = "실리콘밸리 중심에 위치한 우수한 커뮤니티 칼리지입니다. 테크 기업 인턴십 기회가 풍부합니다.",
                pros = listOf("실리콘밸리 위치", "테크 기업 연계", "우수한 STEM 프로그램"),
                cons = listOf("생활비가 높음", "주거 경쟁 치열"),
                featureBadges = listOf("실리콘밸리 위치", "STEM 집중"),
                totalScore = 78.0
            ),
            DefaultSchoolTemplate(
                name = "Diablo Valley College",
                type = "community_college",
                state = "CA",
                city = "Pleasant Hill",
                tuition = 8500,
                programName = baseProgramName,
                degree = "AA",
                duration = "2년",
                recommendationType = "safe",
                explanation = "UC Berkeley 편입률이 높은 커뮤니티 칼리지입니다. 예산 대비 뛰어난 교육 품질을 제공합니다.",
                pros = listOf("UC Berkeley 편입률 1위", "합리적인 학비", "소규모 수업"),
                cons = listOf("대중교통 불편", "도심 외곽 위치"),
                featureBadges = listOf("UC 편입 1위", "소규모 수업"),
                totalScore = 82.0
            ),
            DefaultSchoolTemplate(
                name = "Orange Coast College",
                type = "community_college",
                state = "CA",
                city = "Costa Mesa",
                tuition = 9200,
                programName = baseProgramName,
                degree = "AS",
                duration = "2년",
                recommendationType = "strategy",
                explanation = "오렌지카운티의 대표적인 커뮤니티 칼리지입니다. 다양한 전공과 실무 중심 교육을 제공합니다.",
                pros = listOf("다양한 전공 선택", "좋은 날씨", "해변 근처"),
                cons = listOf("교통 혼잡", "주거비 다소 높음"),
                featureBadges = listOf("다양한 전공", "OPT STEM 인정"),
                totalScore = 75.0
            ),
            DefaultSchoolTemplate(
                name = "Foothill College",
                type = "community_college",
                state = "CA",
                city = "Los Altos Hills",
                tuition = 9800,
                programName = baseProgramName,
                degree = "AA",
                duration = "2년",
                recommendationType = "strategy",
                explanation = "실리콘밸리 남부에 위치한 우수한 커뮤니티 칼리지입니다. 온라인 수업 옵션이 다양합니다.",
                pros = listOf("유연한 수업 일정", "온라인 수업 다양", "아름다운 캠퍼스"),
                cons = listOf("생활비 높음", "대중교통 제한적"),
                featureBadges = listOf("온라인 수업", "아름다운 캠퍼스"),
                totalScore = 73.0
            ),
            DefaultSchoolTemplate(
                name = "Borough of Manhattan Community College",
                type = "community_college",
                state = "NY",
                city = "New York",
                tuition = 7500,
                programName = baseProgramName,
                degree = "AS",
                duration = "2년",
                recommendationType = "safe",
                explanation = "맨해튼 중심의 커뮤니티 칼리지입니다. CUNY 편입 경로가 잘 갖춰져 있고 교통이 편리합니다.",
                pros = listOf("맨해튼 위치", "CUNY 편입", "저렴한 학비", "대중교통 편리"),
                cons = listOf("기숙사 없음", "도심 생활비 높음"),
                featureBadges = listOf("CUNY 편입", "저렴한 학비"),
                totalScore = 80.0
            ),
            DefaultSchoolTemplate(
                name = "LaGuardia Community College",
                type = "community_college",
                state = "NY",
                city = "Long Island City",
                tuition = 6800,
                programName = baseProgramName,
                degree = "AA",
                duration = "2년",
                recommendationType = "safe",
                explanation = "퀸스에 위치한 CUNY 소속 커뮤니티 칼리지입니다. 다양한 이민자 학생들이 편입을 준비합니다.",
                pros = listOf("저렴한 학비", "CUNY 편입", "다문화 환경"),
                cons = listOf("캠퍼스 규모 제한", "주차 불편"),
                featureBadges = listOf("CUNY", "다문화"),
                totalScore = 77.0
            ),
            DefaultSchoolTemplate(
                name = "Kingsborough Community College",
                type = "community_college",
                state = "NY",
                city = "Brooklyn",
                tuition = 7200,
                programName = baseProgramName,
                degree = "AS",
                duration = "2년",
                recommendationType = "strategy",
                explanation = "브루클린의 해변 인근 커뮤니티 칼리지입니다. CUNY 4년제 편입률이 높습니다.",
                pros = listOf("브루클린 위치", "CUNY 편입", "캠퍼스 규모 적당"),
                cons = listOf("대중교통 시간 소요"),
                featureBadges = listOf("CUNY 편입", "해변 캠퍼스"),
                totalScore = 76.0
            ),
            DefaultSchoolTemplate(
                name = "Austin Community College",
                type = "community_college",
                state = "TX",
                city = "Austin",
                tuition = 6800,
                programName = baseProgramName,
                degree = "AA",
                duration = "2년",
                recommendationType = "safe",
                explanation = "오스틴에 위치한 대형 커뮤니티 칼리지입니다. UT Austin 편입 협정이 잘 되어 있습니다.",
                pros = listOf("UT Austin 편입", "저렴한 학비", "오스틴 거주 환경"),
                cons = listOf("캠퍼스 분산", "여름 더움"),
                featureBadges = listOf("UT 편입", "저렴한 학비"),
                totalScore = 81.0
            ),
            DefaultSchoolTemplate(
                name = "Houston Community College",
                type = "community_college",
                state = "TX",
                city = "Houston",
                tuition = 6200,
                programName = baseProgramName,
                degree = "AS",
                duration = "2년",
                recommendationType = "safe",
                explanation = "휴스턴 지역 최대 커뮤니티 칼리지입니다. 다양한 캠퍼스와 전공을 제공합니다.",
                pros = listOf("저렴한 학비", "다양한 캠퍼스", "휴스턴 대학 편입"),
                cons = listOf("도시 규모로 인한 이동 시간"),
                featureBadges = listOf("저렴한 학비", "다캠퍼스"),
                totalScore = 78.0
            ),
            DefaultSchoolTemplate(
                name = "Dallas College",
                type = "community_college",
                state = "TX",
                city = "Dallas",
                tuition = 6500,
                programName = baseProgramName,
                degree = "AA",
                duration = "2년",
                recommendationType = "strategy",
                explanation = "댈러스-포트워스 지역의 커뮤니티 칼리지입니다. 4년제 편입 경로가 다양합니다.",
                pros = listOf("저렴한 학비", "댈러스 지역", "편입 옵션 다양"),
                cons = listOf("캠퍼스별 차이"),
                featureBadges = listOf("저렴한 학비", "편입 다양"),
                totalScore = 75.0
            ),
            DefaultSchoolTemplate(
                name = "San Jose State University",
                type = "university",
                state = "CA",
                city = "San Jose",
                tuition = 18000,
                programName = baseProgramName,
                degree = "BS",
                duration = "4년",
                recommendationType = "challenge",
                explanation = "실리콘밸리 중심의 공립 대학입니다. 테크 기업 채용 연계가 뛰어납니다.",
                pros = listOf("실리콘밸리 위치", "테크 채용", "공립 4년제"),
                cons = listOf("학비 높음", "경쟁률 높음"),
                featureBadges = listOf("STEM 강점", "취업 연계"),
                totalScore = 82.0
            ),
            DefaultSchoolTemplate(
                name = "California State University, Long Beach",
                type = "university",
                state = "CA",
                city = "Long Beach",
                tuition = 16500,
                programName = baseProgramName,
                degree = "BS",
                duration = "4년",
                recommendationType = "strategy",
                explanation = "CSU 시스템의 대형 캠퍼스입니다. 비용 대비 높은 취업률을 자랑합니다.",
                pros = listOf("CSU 시스템", "해변 인근", "다양한 전공"),
                cons = listOf("대형 강의", "주거비"),
                featureBadges = listOf("CSU", "다양한 전공"),
                totalScore = 79.0
            )
        )
    }
    
    /**
     * 기본 추천용 학교 템플릿.
     */
    private data class DefaultSchoolTemplate(
        val name: String,
        val type: String,
        val state: String,
        val city: String,
        val tuition: Int,
        val programName: String,
        val degree: String,
        val duration: String,
        val recommendationType: String,
        val explanation: String,
        val pros: List<String>,
        val cons: List<String>,
        val featureBadges: List<String>,
        val totalScore: Double
    )
}
