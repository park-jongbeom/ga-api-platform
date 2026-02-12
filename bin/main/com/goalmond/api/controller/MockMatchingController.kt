package com.goalmond.api.controller

import com.goalmond.api.domain.dto.ApiResponse
import com.goalmond.api.domain.dto.MatchingResponse
import com.goalmond.api.domain.dto.ProgramListResponse
import com.goalmond.api.domain.dto.ProgramResponse
import com.goalmond.api.domain.dto.SchoolResponse
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID
import kotlin.random.Random

/**
 * Mock 매칭 API Controller.
 * default 프로파일에서만 활성화 (DB 없이 테스트용).
 * local/lightsail 프로파일에서는 MatchingController 사용.
 */
@RestController
@RequestMapping("/api/v1")
@Profile("default", "mock")
class MockMatchingController {

    private var lastMatchingResult: MatchingResponse? = null

    data class MatchingRunRequest(
        val userId: String
    )

    @PostMapping("/matching/run")
    fun runMatching(@RequestBody request: MatchingRunRequest): ResponseEntity<ApiResponse<MatchingResponse>> {
        val scenario = MockScenario.values().random()
        val response = scenario.toResponse(request.userId)
        lastMatchingResult = response
        return ResponseEntity.ok(ApiResponse(success = true, data = response))
    }

    @GetMapping("/matching/result")
    fun getLatestMatchingResult(): ResponseEntity<ApiResponse<MatchingResponse>> {
        if (lastMatchingResult == null) {
            return errorResponse(
                status = HttpStatus.NOT_FOUND,
                code = "MATCHING_RESULT_NOT_FOUND",
                message = "매칭 결과가 없습니다."
            )
        }
        return ResponseEntity.ok(ApiResponse(success = true, data = lastMatchingResult))
    }

    @GetMapping("/programs")
    fun getPrograms(
        @RequestParam type: String,
        @RequestParam(required = false) state: String?,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<ApiResponse<ProgramListResponse>> {
        val allowedTypes = setOf("university", "community_college", "vocational")
        if (type !in allowedTypes) {
            return errorResponse(
                status = HttpStatus.BAD_REQUEST,
                code = "INVALID_PROGRAM_TYPE",
                message = "허용되지 않은 프로그램 유형입니다."
            )
        }

        val programs = (1..size).map { index ->
            ProgramResponse(
                id = "program-${page}-${index}",
                schoolId = "school-${index.toString().padStart(3, '0')}",
                schoolName = "${state ?: "CA"} Community College #$index",
                programName = "Computer Science ${listOf("AA", "AS")[index % 2]}",
                type = type,
                degree = if (index % 2 == 0) "AA" else "AS",
                duration = "2 years",
                tuition = 16000 + (index * 500),
                state = state ?: "CA",
                city = listOf("Irvine", "Santa Monica", "Cupertino", "San Diego")[index % 4],
                optAvailable = true,
                transferRate = 60 + (index % 30),
                careerPath = "Software Developer, Web Developer"
            )
        }

        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                data = ProgramListResponse(
                    total = 45,
                    page = page,
                    size = size,
                    programs = programs
                )
            )
        )
    }

    @GetMapping("/schools/{schoolId}")
    fun getSchoolDetail(
        @PathVariable schoolId: String
    ): ResponseEntity<ApiResponse<SchoolResponse>> {
        val response = SchoolResponse(
            id = schoolId,
            name = "Irvine Valley College",
            type = "community_college",
            state = "CA",
            city = "Irvine",
            tuition = 18000,
            livingCost = 15000,
            ranking = 15,
            description = "Irvine Valley College is a premier community college in Orange County...",
            campusInfo = "Modern campus with state-of-the-art facilities",
            dormitory = false,
            dining = true,
            programs = listOf(
                SchoolResponse.ProgramSummary(
                    id = "program-001",
                    name = "Computer Science AA",
                    degree = "AA",
                    duration = "2 years"
                ),
                SchoolResponse.ProgramSummary(
                    id = "program-002",
                    name = "Business Administration AS",
                    degree = "AS",
                    duration = "2 years"
                )
            ),
            acceptanceRate = 45,
            transferRate = 75,
            graduationRate = 68,
            images = listOf(
                "https://cdn.goalmond.com/schools/ivc-campus-1.jpg",
                "https://cdn.goalmond.com/schools/ivc-campus-2.jpg"
            ),
            website = "https://www.ivc.edu",
            contact = SchoolResponse.Contact(
                email = "admissions@ivc.edu",
                phone = "+1-949-451-5100",
                address = "5500 Irvine Center Dr, Irvine, CA 92618"
            )
        )

        return ResponseEntity.ok(ApiResponse(success = true, data = response))
    }

    private enum class MockScenario {
        SAFE,
        CHALLENGE,
        STRATEGY;

        fun toResponse(userId: String): MatchingResponse {
            val now = Instant.now()
            val executionTimeMs = Random.nextInt(1800, 3200)
            val results = when (this) {
                SAFE -> safeResults()
                CHALLENGE -> challengeResults()
                STRATEGY -> strategyResults()
            }

            return MatchingResponse(
                matchingId = UUID.randomUUID().toString(),
                userId = userId,
                totalMatches = results.size,
                executionTimeMs = executionTimeMs,
                results = results,
                createdAt = now,
                indicatorDescription = "학업 적합도와 진로 전망에서 가장 높은 적합성을 보이며, 비용 효율성도 안정적입니다.",
                nextSteps = listOf(
                    MatchingResponse.NextStep(
                        id = 1,
                        title = "서류 심사",
                        description = "GPA 및 시험 점수를 업로드하여 최종 검토를 진행하세요.",
                        priority = "recommended"
                    ),
                    MatchingResponse.NextStep(
                        id = 2,
                        title = "SOP 워크숍",
                        description = "전문 편집자와의 세션을 예약해 지원서를 고도화하세요.",
                        priority = "recommended"
                    ),
                    MatchingResponse.NextStep(
                        id = 3,
                        title = "지원 포털 접근",
                        description = "학교별 마감일에 맞춰 공통 지원서를 제출하세요.",
                        priority = "recommended"
                    ),
                    MatchingResponse.NextStep(
                        id = 4,
                        title = "비자 준비",
                        description = "I-20 서류 준비 및 인터뷰 일정을 미리 계획하세요.",
                        priority = "optional"
                    )
                )
            )
        }

        private fun safeResults(): List<MatchingResponse.MatchingResult> {
            return listOf(
                buildResult(
                    rank = 1,
                    schoolId = "school-001",
                    schoolName = "Irvine Valley College",
                    city = "Irvine",
                    totalScore = 87.5,
                    recommendationType = "safe",
                    explanation = "예산 대비 학비가 안정적이며 영어 점수가 충분하여 추천됩니다.",
                    pros = listOf(
                        "예산 여유 충분 ($7,000)",
                        "영어 점수 입학 기준 초과 (TOEFL 95 vs 70)",
                        "OPT 가능",
                        "높은 편입 성공률 (75%)"
                    ),
                    cons = listOf("경쟁률 다소 높음 (45%)")
                ),
                buildResult(
                    rank = 2,
                    schoolId = "school-002",
                    schoolName = "Santa Monica College",
                    city = "Santa Monica",
                    totalScore = 84.2,
                    recommendationType = "safe",
                    explanation = "명문 편입 학교로 UCLA 편입률이 높아 추천됩니다.",
                    pros = listOf("UCLA 편입률 1위", "캠퍼스 위치 우수", "OPT 가능"),
                    cons = listOf("예산 임계 ($5,000 여유)", "경쟁률 높음 (35%)")
                ),
                buildResult(
                    rank = 3,
                    schoolId = "school-003",
                    schoolName = "De Anza College",
                    city = "Cupertino",
                    totalScore = 82.8,
                    recommendationType = "challenge",
                    explanation = "실리콘밸리 중심에 위치하여 IT 기업 인턴십 기회가 많습니다.",
                    pros = listOf("실리콘밸리 위치", "IT 기업 네트워킹 우수", "예산 적합"),
                    cons = listOf("선호 도시와 거리 있음")
                ),
                buildResult(
                    rank = 4,
                    schoolId = "school-004",
                    schoolName = "Foothill College",
                    city = "Los Altos Hills",
                    totalScore = 80.4,
                    recommendationType = "challenge",
                    explanation = "IT 전공 커리큘럼이 탄탄하고 편입 지원이 활발합니다.",
                    pros = listOf("편입 지원 프로그램 우수", "STEM 중심 커리큘럼"),
                    cons = listOf("생활비가 다소 높음")
                ),
                buildResult(
                    rank = 5,
                    schoolId = "school-005",
                    schoolName = "Pasadena City College",
                    city = "Pasadena",
                    totalScore = 78.9,
                    recommendationType = "strategy",
                    explanation = "입학 난이도가 적절하고 지역 접근성이 좋습니다.",
                    pros = listOf("입학 요건 완화", "지역 접근성 우수"),
                    cons = listOf("OPT 연계 정보 부족")
                )
            )
        }

        private fun challengeResults(): List<MatchingResponse.MatchingResult> {
            return listOf(
                buildResult(
                    rank = 1,
                    schoolId = "school-006",
                    schoolName = "Orange Coast College",
                    city = "Costa Mesa",
                    totalScore = 74.6,
                    recommendationType = "challenge",
                    explanation = "예산 대비 학비는 적정하나 영어 점수 보완이 필요합니다.",
                    pros = listOf("학비 부담 적음", "커뮤니티 네트워크 강점"),
                    cons = listOf("영어 점수 추가 필요", "경쟁률 상승")
                ),
                buildResult(
                    rank = 2,
                    schoolId = "school-007",
                    schoolName = "College of San Mateo",
                    city = "San Mateo",
                    totalScore = 72.3,
                    recommendationType = "challenge",
                    explanation = "입학 조건은 적합하나 원하는 지역과 거리가 있습니다.",
                    pros = listOf("학업 지원 우수", "편입 프로그램 운영"),
                    cons = listOf("거리 문제", "생활비 상승")
                ),
                buildResult(
                    rank = 3,
                    schoolId = "school-008",
                    schoolName = "Diablo Valley College",
                    city = "Pleasant Hill",
                    totalScore = 71.2,
                    recommendationType = "strategy",
                    explanation = "편입률이 높아 장기적으로 유리합니다.",
                    pros = listOf("편입률 1위권", "학습 환경 안정"),
                    cons = listOf("예산 여유 적음")
                ),
                buildResult(
                    rank = 4,
                    schoolId = "school-009",
                    schoolName = "Glendale Community College",
                    city = "Glendale",
                    totalScore = 69.8,
                    recommendationType = "strategy",
                    explanation = "중간 난이도의 전략적 선택입니다.",
                    pros = listOf("입학 난이도 중간"),
                    cons = listOf("OPT 가능성 불확실")
                ),
                buildResult(
                    rank = 5,
                    schoolId = "school-010",
                    schoolName = "Fullerton College",
                    city = "Fullerton",
                    totalScore = 68.4,
                    recommendationType = "strategy",
                    explanation = "예산에 맞추기 위해 학비가 낮은 프로그램을 추천합니다.",
                    pros = listOf("학비 저렴"),
                    cons = listOf("프로그램 선택 제한")
                )
            )
        }

        private fun strategyResults(): List<MatchingResponse.MatchingResult> {
            return listOf(
                buildResult(
                    rank = 1,
                    schoolId = "school-011",
                    schoolName = "Southwestern College",
                    city = "Chula Vista",
                    totalScore = 66.2,
                    recommendationType = "strategy",
                    explanation = "예산과 기간에 최적화된 전략적 경로입니다.",
                    pros = listOf("예산 적합", "입학 요건 낮음"),
                    cons = listOf("추천 순위 중간")
                ),
                buildResult(
                    rank = 2,
                    schoolId = "school-012",
                    schoolName = "Riverside City College",
                    city = "Riverside",
                    totalScore = 64.7,
                    recommendationType = "strategy",
                    explanation = "생활비 절감과 학업 목표를 동시에 고려했습니다.",
                    pros = listOf("생활비 저렴", "학업 기간 조정 가능"),
                    cons = listOf("선호 지역과 거리 있음")
                ),
                buildResult(
                    rank = 3,
                    schoolId = "school-013",
                    schoolName = "Los Angeles City College",
                    city = "Los Angeles",
                    totalScore = 63.9,
                    recommendationType = "strategy",
                    explanation = "도심 접근성을 고려한 전략적 선택입니다.",
                    pros = listOf("도심 접근성", "다양한 프로그램"),
                    cons = listOf("경쟁률 있음")
                ),
                buildResult(
                    rank = 4,
                    schoolId = "school-014",
                    schoolName = "San Diego Mesa College",
                    city = "San Diego",
                    totalScore = 62.4,
                    recommendationType = "strategy",
                    explanation = "장기적인 경력 계획에 맞춘 대안입니다.",
                    pros = listOf("커리어 연계성"),
                    cons = listOf("예산 압박")
                ),
                buildResult(
                    rank = 5,
                    schoolId = "school-015",
                    schoolName = "Sacramento City College",
                    city = "Sacramento",
                    totalScore = 60.8,
                    recommendationType = "strategy",
                    explanation = "안정적인 입학과 비용 절감을 우선시합니다.",
                    pros = listOf("입학 가능성 높음"),
                    cons = listOf("추천 점수 낮음")
                )
            )
        }

        private fun buildResult(
            rank: Int,
            schoolId: String,
            schoolName: String,
            city: String,
            totalScore: Double,
            recommendationType: String,
            explanation: String,
            pros: List<String>,
            cons: List<String>
        ): MatchingResponse.MatchingResult {
            return MatchingResponse.MatchingResult(
                rank = rank,
                school = MatchingResponse.SchoolSummary(
                    id = schoolId,
                    name = schoolName,
                    type = "community_college",
                    state = "CA",
                    city = city,
                    tuition = 18000 + (rank * 500),
                    imageUrl = "https://cdn.goalmond.com/schools/${schoolId}.jpg"
                ),
                program = MatchingResponse.ProgramSummary(
                    id = "program-$schoolId",
                    name = "Computer Science ${if (rank % 2 == 0) "AS" else "AA"}",
                    degree = if (rank % 2 == 0) "AS" else "AA",
                    duration = "2 years",
                    optAvailable = true
                ),
                totalScore = totalScore,
                estimatedRoi = 12.0 + rank,
                scoreBreakdown = MatchingResponse.ScoreBreakdown(
                    academic = 14 + rank,
                    english = 12 + rank,
                    budget = 13 + rank,
                    location = 8 + rank,
                    duration = 7 + rank,
                    career = 20 + rank
                ),
                indicatorScores = MatchingResponse.IndicatorScores(
                    academicFit = 13 + rank,
                    careerOutlook = 14 + rank,
                    costEfficiency = 10 + rank
                ),
                recommendationType = recommendationType,
                explanation = explanation,
                pros = pros,
                cons = cons
            )
        }
    }

    private fun <T> errorResponse(
        status: HttpStatus,
        code: String,
        message: String
    ): ResponseEntity<ApiResponse<T>> {
        return ResponseEntity.status(status).body(
            ApiResponse(
                success = false,
                code = code,
                message = message,
                timestamp = Instant.now()
            )
        )
    }
}
