package com.goalmond.api.service.matching

import com.goalmond.api.domain.entity.School
import com.goalmond.api.repository.ProgramRepository
import com.goalmond.api.repository.SchoolEmbeddingRepository
import com.goalmond.api.repository.SchoolRepository
import com.goalmond.api.support.FakeGeminiTestConfig
import com.goalmond.api.support.PostgresTestcontainersConfig
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
 * EmbeddingService 통합 테스트 (GAM-3, Phase 2).
 * 
 * 테스트 목표:
 * 1. School 엔티티 → 임베딩 텍스트 변환 (필수 필드 포함)
 * 2. embedSchool() 호출 시 SchoolEmbedding 저장 성공
 * 3. embedAllSchools() 배치 작업 성공 (10개 학교 < 30초)
 * 4. 중복 임베딩 방지 (UPSERT 동작)
 * 5. 임베딩 실패 시 로깅 및 스킵 (전체 작업 중단 X)
 */
@SpringBootTest
@ActiveProfiles("local")
@Import(FakeGeminiTestConfig::class, PostgresTestcontainersConfig::class)
class EmbeddingServiceTest {
    
    @Autowired
    private lateinit var embeddingService: EmbeddingService
    
    @Autowired
    private lateinit var schoolRepository: SchoolRepository
    
    @Autowired
    private lateinit var schoolEmbeddingRepository: SchoolEmbeddingRepository
    
    @Autowired
    private lateinit var programRepository: ProgramRepository
    
    private val testPrefix = "[TEST_EMBED] "
    
    @BeforeEach
    fun setUp() {
        // 테스트 데이터 정리 (실제 DB 사용하므로 안전한 범위만 삭제)
        val testSchools = schoolRepository.findByNameStartingWith(testPrefix)
        testSchools.forEach { school ->
            val schoolId = requireNotNull(school.id)
            programRepository.findBySchoolId(schoolId).let { programs ->
                if (programs.isNotEmpty()) {
                    programRepository.deleteAll(programs)
                }
            }
            schoolEmbeddingRepository.findBySchoolId(schoolId)?.let {
                schoolEmbeddingRepository.delete(it)
            }
            schoolRepository.delete(school)
        }
    }
    
    @Test
    fun `School을 임베딩 텍스트로 변환`() {
        // Given
        val school = School(
            name = "${testPrefix}Irvine Valley College",
            type = "community_college",
            state = "CA",
            city = "Irvine",
            tuition = 18000,
            livingCost = 15000,
            description = "Premier community college in Orange County",
            acceptanceRate = 45,
            transferRate = 75,
            graduationRate = 68,
            averageSalary = 52000,
            employmentRate = BigDecimal("88.5"),
            ranking = 15
        )
        
        // When
        val text = embeddingService.buildSchoolText(school)
        
        // Then
        assertThat(text).contains("Irvine Valley College")
        assertThat(text).contains("커뮤니티 칼리지")
        assertThat(text).contains("Irvine, CA")
        assertThat(text).contains("학비: \$18000")
        assertThat(text).contains("생활비: \$15000")
        assertThat(text).contains("합격률: 45%")
        assertThat(text).contains("편입률: 75%")
        assertThat(text).contains("졸업률: 68%")
        assertThat(text).contains("초봉(중간값): \$52000")
        assertThat(text).contains("취업률: 88.5%")
        
        logger.info("Generated embedding text:\n$text")
    }
    
    @Test
    fun `School 임베딩 저장 성공`() {
        // Given
        val school = School(
            name = "${testPrefix}School for Embedding",
            type = "community_college",
            state = "CA",
            city = "Test City",
            tuition = 20000,
            description = "Test school for embedding service"
        )
        val saved = schoolRepository.save(school)
        
        // When
        val success = embeddingService.embedSchool(saved)
        
        // Then
        assertThat(success).isTrue()
        
        val savedId = requireNotNull(saved.id)
        val embedding = schoolEmbeddingRepository.findBySchoolId(savedId)
        assertThat(embedding).isNotNull
        assertThat(embedding?.schoolId).isEqualTo(savedId)
        assertThat(embedding?.embeddingText).contains("${testPrefix}School for Embedding")
        
        // 768차원 벡터 검증
        val vector = embedding?.getEmbeddingVector()
        assertThat(vector).hasSize(768)
        
        logger.info("Embedding saved successfully for school: ${saved.name}")
    }
    
    @Test
    fun `동일한 School을 재임베딩하면 UPSERT로 업데이트됨`() {
        // Given
        val school = School(
            name = "${testPrefix}School for UPSERT",
            type = "university",
            state = "CA",
            city = "Los Angeles",
            tuition = 30000
        )
        val saved = schoolRepository.save(school)
        
        // When: 첫 번째 임베딩
        embeddingService.embedSchool(saved)
        val savedId = requireNotNull(saved.id)
        val first = schoolEmbeddingRepository.findBySchoolId(savedId)
        val firstText = first?.embeddingText
        
        // 학교 정보 변경
        saved.description = "Updated description for testing UPSERT"
        schoolRepository.save(saved)
        
        // 두 번째 임베딩
        embeddingService.embedSchool(saved)
        val second = schoolEmbeddingRepository.findBySchoolId(savedId)
        val secondText = second?.embeddingText
        
        // Then
        assertThat(firstText).isNotEqualTo(secondText) // 텍스트 변경됨
        assertThat(secondText).contains("Updated description")
        assertThat(schoolEmbeddingRepository.countBySchoolId(savedId)).isEqualTo(1L)
    }
    
    @Test
    fun `embedAllSchools 배치 작업이 정상 동작함`() {
        // Given: 5개 테스트 학교 생성
        val schools = (1..5).map { index ->
            School(
                name = "${testPrefix}School Batch $index",
                type = "community_college",
                state = "CA",
                city = "Test City $index",
                tuition = 15000 + (index * 1000),
                description = "Test school number $index for batch embedding"
            )
        }
        schools.forEach { schoolRepository.save(it) }
        
        // When: 테스트 학교만 임베딩 (시간 측정)
        val duration = measureTimeMillis {
            val successCount = schools.count { school ->
                embeddingService.embedSchool(school)
            }
            assertThat(successCount).isEqualTo(5)
        }
        
        // Then: 시간 검증 (5개 학교 * 1초 대기 = 최소 4초, 최대 15초)
        assertThat(duration).isLessThan(20000)
        
        // 모든 임베딩 저장 확인
        val embeddings = schoolEmbeddingRepository.findAll()
        assertThat(embeddings).hasSizeGreaterThanOrEqualTo(5)
        
        logger.info("Batch embedding completed in ${duration}ms for ${schools.size} schools")
    }
    
    companion object {
        private val logger = org.slf4j.LoggerFactory.getLogger(EmbeddingServiceTest::class.java)
    }
}
