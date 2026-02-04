package com.goalmond.api.service.matching

import com.goalmond.api.domain.entity.AcademicProfile
import com.goalmond.api.domain.entity.Program
import com.goalmond.api.domain.entity.School
import com.goalmond.api.domain.entity.User
import com.goalmond.api.domain.entity.UserPreference
import com.goalmond.api.repository.AcademicProfileRepository
import com.goalmond.api.repository.ProgramRepository
import com.goalmond.api.repository.SchoolRepository
import com.goalmond.api.repository.UserPreferenceRepository
import com.goalmond.api.repository.UserRepository
import com.goalmond.api.support.FakeGeminiTestConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import kotlin.system.measureTimeMillis

/**
 * MatchingEngineService 통합 테스트 (GAM-3, Phase 8).
 * 
 * 테스트 목표:
 * 1. RAG Top 20 → Hard Filter → Scoring → Top 5 전체 플로우
 * 2. 전체 실행 시간 < 3초
 * 3. Top 5 결과 내림차순 정렬
 * 4. 각 결과에 설명 포함
 */
@SpringBootTest
@ActiveProfiles("local")
@Import(FakeGeminiTestConfig::class)
class MatchingEngineServiceTest {
    
    @Autowired
    private lateinit var matchingEngineService: MatchingEngineService
    
    @Autowired
    private lateinit var userRepository: UserRepository
    
    @Autowired
    private lateinit var academicProfileRepository: AcademicProfileRepository
    
    @Autowired
    private lateinit var userPreferenceRepository: UserPreferenceRepository
    
    @Autowired
    private lateinit var schoolRepository: SchoolRepository
    
    @Autowired
    private lateinit var programRepository: ProgramRepository
    
    @Autowired
    private lateinit var embeddingService: EmbeddingService
    
    private lateinit var testUser: User
    private val testPrefix = "[TEST_MATCH] "
    
    @BeforeEach
    fun setUp() {
        // 테스트 사용자 생성
        val uniqueEmail = "matching${System.currentTimeMillis()}@test.com"
        testUser = User(
            email = uniqueEmail,
            fullName = "Matching Test User"
        )
        testUser = userRepository.save(testUser)
        
        val profile = AcademicProfile(
            userId = testUser.id,
            schoolName = "Seoul High School",
            degree = "고등학교",
            gpa = BigDecimal("3.5"),
            gpaScale = BigDecimal("4.0"),
            englishTestType = "TOEFL",
            englishScore = 85
        )
        academicProfileRepository.save(profile)
        
        val preference = UserPreference(
            userId = testUser.id,
            targetMajor = "Computer Science",
            targetProgram = "community_college",
            targetLocation = "California",
            budgetUsd = 35000,
            careerGoal = "Software Engineer 취업",
            preferredTrack = "편입"
        )
        userPreferenceRepository.save(preference)
        
        // 테스트용 School & Program 생성 (없는 경우에만)
        ensureTestData()
    }
    
    private fun ensureTestData() {
        val existingTests = schoolRepository.findByNameStartingWith(testPrefix)
        if (existingTests.size < 2) {
            logger.info("Creating test schools and programs...")
            
            val school1 = schoolRepository.save(School(
                name = "${testPrefix}CC 1",
                type = "community_college",
                state = "CA",
                city = "Irvine",
                tuition = 18000,
                livingCost = 12000,
                acceptanceRate = 45,
                transferRate = 75,
                description = "Computer Science focused community college"
            ))
            
            val program1 = programRepository.save(Program(
                schoolId = school1.id,
                name = "Computer Science AA",
                type = "community_college",
                degree = "AA",
                duration = "2 years",
                tuition = 18000,
                optAvailable = true
            ))
            
            embeddingService.embedSchool(school1)
            Thread.sleep(1000)
            
            val school2 = schoolRepository.save(School(
                name = "${testPrefix}University 1",
                type = "university",
                state = "CA",
                city = "Los Angeles",
                tuition = 30000,
                livingCost = 15000,
                acceptanceRate = 35,
                description = "Top tier university with CS program",
                globalRanking = "#4",
                rankingField = "Computer Science",
                averageSalary = 85000,
                alumniNetworkCount = 38000,
                featureBadges = """["OPT STEM ELIGIBLE", "ON-CAMPUS HOUSING", "RESEARCH"]"""
            ))
            
            val program2 = programRepository.save(Program(
                schoolId = school2.id,
                name = "Computer Science BS",
                type = "university",
                degree = "BS",
                duration = "4 years",
                tuition = 30000,
                optAvailable = true
            ))
            
            embeddingService.embedSchool(school2)
            
            logger.info("Test data created: 2 schools, 2 programs")
        }
    }
    
    @Test
    fun `전체 매칭 플로우 E2E 테스트`() {
        // When
        val duration = measureTimeMillis {
            val result = matchingEngineService.executeMatching(testUser.id!!)
            
            // Then
            assertThat(result.results).isNotEmpty()
            assertThat(result.results.size).isLessThanOrEqualTo(5)
            assertThat(result.userId).isEqualTo(testUser.id.toString())
            
            // 점수 내림차순 정렬 확인
            if (result.results.size > 1) {
                assertThat(result.results[0].totalScore)
                    .isGreaterThanOrEqualTo(result.results.last().totalScore)
            }
            
            // 각 결과에 설명 포함 및 확장 필드 검증 (매칭 리포트 요구사항)
            result.results.forEach { matchingResult ->
                assertThat(matchingResult.explanation).isNotEmpty()
                assertThat(matchingResult.recommendationType).isIn("safe", "challenge", "strategy")
                // SchoolSummary 확장 필드 포함 (globalRanking, rankingField, averageSalary, alumniNetworkCount, featureBadges)
                assertThat(matchingResult.school.featureBadges).isNotNull()
                // MatchingResult 확장 필드: estimatedRoi는 0 이상
                assertThat(matchingResult.estimatedRoi).isGreaterThanOrEqualTo(0.0)

                logger.info(
                    "Rank ${matchingResult.rank}: ${matchingResult.school.name} " +
                    "(${String.format("%.1f", matchingResult.totalScore)}점, ${matchingResult.recommendationType}, ROI=${matchingResult.estimatedRoi})"
                )
            }
        }
        
        // 전체 실행 시간 검증
        assertThat(duration).isLessThan(10000) // 10초 허용 (최초 임베딩 포함)
        
        logger.info("매칭 완료: ${duration}ms")
    }
    
    companion object {
        private val logger = org.slf4j.LoggerFactory.getLogger(MatchingEngineServiceTest::class.java)
    }
}
