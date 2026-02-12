package com.goalmond.api.integration

import com.goalmond.api.domain.entity.*
import com.goalmond.api.repository.*
import com.goalmond.api.service.matching.EmbeddingService
import com.goalmond.api.service.matching.MatchingEngineService
import com.goalmond.api.service.matching.RAGService
import com.goalmond.api.service.matching.ScoreBreakdown
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * RAG 통합 테스트.
 * 
 * Spring AI VectorStore, CustomGeminiChatModel, RAGService를 통합 테스트합니다.
 * 
 * 주의: 실제 Gemini API를 호출하므로 GEMINI_API_KEY 환경변수 필요.
 * Rate Limiting 주의 (60 req/min).
 */
@SpringBootTest
@ActiveProfiles("local")  // local 프로파일 사용 (DB 필요)
@Transactional
@Disabled("실제 API 호출 테스트이므로 수동 실행")
class RAGIntegrationTest {
    
    @Autowired
    lateinit var embeddingService: EmbeddingService
    
    @Autowired
    lateinit var ragService: RAGService
    
    @Autowired
    lateinit var matchingEngineService: MatchingEngineService
    
    @Autowired
    lateinit var schoolRepository: SchoolRepository
    
    @Autowired
    lateinit var programRepository: ProgramRepository
    
    @Autowired
    lateinit var userRepository: UserRepository
    
    @Autowired
    lateinit var academicProfileRepository: AcademicProfileRepository
    
    @Autowired
    lateinit var userPreferenceRepository: UserPreferenceRepository
    
    @Test
    fun `School 문서 임베딩 및 RAG 검색 테스트`() {
        // Given: 테스트 학교 생성
        val school = School(
            name = "Santa Monica College",
            type = "community_college",
            state = "California",
            city = "Santa Monica",
            tuition = 8000,
            livingCost = 15000,
            acceptanceRate = 100,
            transferRate = 80,
            description = "California의 우수한 커뮤니티 칼리지"
        )
        val savedSchool = schoolRepository.save(school)
        
        // When: 문서 임베딩
        val success = embeddingService.embedSchoolDocument(
            schoolId = savedSchool.id!!,
            docType = "review",
            title = "한국 학생 후기",
            content = "예산 대비 만족도 매우 높음. 편입 지원 우수. 한국인 커뮤니티 활발. UC 계열 편입률 85%.",
            metadata = mapOf("rating" to 4.5, "year" to 2025)
        )
        
        // Then: 임베딩 성공 확인
        assertThat(success).isTrue()
    }
    
    @Test
    fun `RAG 기반 매칭 설명 생성 테스트`() {
        // Given: 테스트 데이터 준비
        val user = createTestUser()
        val profile = createTestAcademicProfile(user)
        val preference = createTestUserPreference(user)
        val school = createTestSchool()
        val program = createTestProgram(school)
        
        // 테스트 문서 추가
        embeddingService.embedSchoolDocument(
            schoolId = school.id!!,
            docType = "review",
            title = "한국 학생 후기",
            content = "예산 대비 만족도 높음. CS 전공 편입률 82%.",
            metadata = mapOf("rating" to 4.5)
        )
        
        // When: RAG 설명 생성
        val explanation = ragService.generateExplanationWithRAG(
            profile = profile,
            preference = preference,
            program = program,
            school = school,
            scores = createTestScores()
        )
        
        // Then: 설명이 생성되었는지 확인
        assertThat(explanation).isNotBlank()
        assertThat(explanation.length).isGreaterThan(50)
        assertThat(explanation).contains("이 학교")  // 프롬프트 지시 확인
    }
    
    @Test
    fun `전체 매칭 플로우 RAG 통합 테스트`() {
        // Given: 완전한 테스트 데이터
        val user = createTestUser()
        val profile = createTestAcademicProfile(user)
        val preference = createTestUserPreference(user)
        val school = createTestSchool()
        
        // 테스트 문서 추가
        embeddingService.embedSchoolDocument(
            schoolId = school.id!!,
            docType = "statistics",
            title = "편입 통계",
            content = "최근 3년간 CS 전공 졸업생의 82%가 UC 계열로 편입 성공.",
            metadata = mapOf("year" to 2024)
        )
        
        // When: 전체 매칭 실행
        val startTime = System.currentTimeMillis()
        val result = matchingEngineService.executeMatching(user.id!!)
        val executionTime = System.currentTimeMillis() - startTime
        
        // Then: 매칭 결과 검증
        assertThat(result.totalMatches).isGreaterThan(0)
        assertThat(result.executionTimeMs).isLessThan(3000)  // 3초 이내
        
        // RAG 설명 포함 확인
        val firstResult = result.results.firstOrNull()
        assertThat(firstResult).isNotNull()
        assertThat(firstResult?.explanation).isNotBlank()
        
        println("매칭 실행 시간: ${executionTime}ms")
        println("설명: ${firstResult?.explanation}")
    }
    
    @Test
    fun `배치 문서 임베딩 성능 테스트`() {
        // Given: 테스트 학교
        val school = createTestSchool()
        
        val documents = listOf(
            mapOf(
                "docType" to "review",
                "title" to "한국 학생 후기 1",
                "content" to "좋은 학교입니다. 편입 지원 우수."
            ),
            mapOf(
                "docType" to "admission_guide",
                "title" to "입학 가이드",
                "content" to "GPA 3.0 이상 권장. TOEFL 80 이상."
            ),
            mapOf(
                "docType" to "statistics",
                "title" to "편입 통계",
                "content" to "편입률 85%. UC 계열 편입 다수."
            )
        )
        
        // When: 배치 임베딩
        val startTime = System.currentTimeMillis()
        
        documents.forEach { doc ->
            embeddingService.embedSchoolDocument(
                schoolId = school.id!!,
                docType = doc["docType"] as String,
                title = doc["title"] as String,
                content = doc["content"] as String
            )
            Thread.sleep(1000)  // Rate limiting
        }
        
        val elapsed = System.currentTimeMillis() - startTime
        
        // Then: 성능 확인
        println("배치 임베딩 시간: ${elapsed}ms (${documents.size}개 문서)")
        assertThat(elapsed).isLessThan(5000)  // 5초 이내 (3개 문서)
    }
    
    // ====== Helper 메서드 ======
    
    private fun createTestUser(): User {
        val user = User(
            email = "test-rag@example.com",
            fullName = "RAG Test User",
            passwordHash = "dummy",
            isActive = true
        )
        return userRepository.save(user)
    }
    
    private fun createTestAcademicProfile(user: User): AcademicProfile {
        val profile = AcademicProfile(
            userId = user.id,
            schoolName = "Test High School",
            degree = "BACHELOR",
            major = "Computer Science",
            gpa = java.math.BigDecimal("3.5"),
            gpaScale = java.math.BigDecimal("4.0"),
            englishTestType = "TOEFL",
            englishScore = 90
        )
        return academicProfileRepository.save(profile)
    }
    
    private fun createTestUserPreference(user: User): UserPreference {
        val preference = UserPreference(
            userId = user.id,
            targetMajor = "Computer Science",
            targetProgram = "CC",
            targetLocation = "California",
            budgetUsd = 30000,
            careerGoal = "Software Engineer",
            preferredTrack = "TRANSFER"
        )
        return userPreferenceRepository.save(preference)
    }
    
    private fun createTestSchool(): School {
        val school = School(
            name = "Test Community College",
            type = "community_college",
            state = "California",
            city = "Los Angeles",
            tuition = 8000,
            livingCost = 15000,
            acceptanceRate = 100,
            transferRate = 80,
            graduationRate = 70,
            description = "Test college for RAG"
        )
        return schoolRepository.save(school)
    }
    
    private fun createTestProgram(school: School): Program {
        val program = Program(
            schoolId = school.id,
            name = "Computer Science AS",
            type = "CC",
            degree = "Associate",
            duration = "2 years",
            tuition = 8000,
            optAvailable = true
        )
        return programRepository.save(program)
    }
    
    private fun createTestScores(): ScoreBreakdown = ScoreBreakdown(
        academic = 18.0,
        english = 14.0,
        budget = 13.0,
        location = 9.0,
        duration = 8.0,
        career = 25.0
    )
}
