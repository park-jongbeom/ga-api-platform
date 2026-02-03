package com.goalmond.api.service.matching

import com.goalmond.api.domain.entity.School
import com.goalmond.api.repository.SchoolEmbeddingRepository
import com.goalmond.api.repository.SchoolRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
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
class EmbeddingServiceTest {
    
    @Autowired
    private lateinit var embeddingService: EmbeddingService
    
    @Autowired
    private lateinit var schoolRepository: SchoolRepository
    
    @Autowired
    private lateinit var schoolEmbeddingRepository: SchoolEmbeddingRepository
    
    @BeforeEach
    fun setUp() {
        // 테스트 데이터 정리 (실제 DB 사용하므로 조심스럽게)
        // 테스트 학교만 삭제 (이름이 "Test School"로 시작하는 것들)
        val testSchools = schoolRepository.findAll().filter { it.name.startsWith("Test School") }
        testSchools.forEach { school ->
            schoolEmbeddingRepository.findBySchoolId(school.id!!)?.let {
                schoolEmbeddingRepository.delete(it)
            }
            schoolRepository.delete(school)
        }
    }
    
    @Test
    fun `School을 임베딩 텍스트로 변환`() {
        // Given
        val school = School(
            name = "Irvine Valley College",
            type = "community_college",
            state = "CA",
            city = "Irvine",
            tuition = 18000,
            livingCost = 15000,
            description = "Premier community college in Orange County",
            acceptanceRate = 45,
            transferRate = 75,
            graduationRate = 68,
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
        
        logger.info("Generated embedding text:\n$text")
    }
    
    @Test
    fun `School 임베딩 저장 성공`() {
        // Given
        val school = School(
            name = "Test School for Embedding",
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
        
        val embedding = schoolEmbeddingRepository.findBySchoolId(saved.id!!)
        assertThat(embedding).isNotNull
        assertThat(embedding?.schoolId).isEqualTo(saved.id)
        assertThat(embedding?.embeddingText).contains("Test School for Embedding")
        
        // 768차원 벡터 검증
        val vector = embedding?.getEmbeddingVector()
        assertThat(vector).hasSize(768)
        
        logger.info("Embedding saved successfully for school: ${saved.name}")
    }
    
    @Test
    fun `동일한 School을 재임베딩하면 UPSERT로 업데이트됨`() {
        // Given
        val school = School(
            name = "Test School for UPSERT",
            type = "university",
            state = "CA",
            city = "Los Angeles",
            tuition = 30000
        )
        val saved = schoolRepository.save(school)
        
        // When: 첫 번째 임베딩
        embeddingService.embedSchool(saved)
        val first = schoolEmbeddingRepository.findBySchoolId(saved.id!!)
        val firstText = first?.embeddingText
        
        // 학교 정보 변경
        saved.description = "Updated description for testing UPSERT"
        schoolRepository.save(saved)
        
        // 두 번째 임베딩
        embeddingService.embedSchool(saved)
        val second = schoolEmbeddingRepository.findBySchoolId(saved.id!!)
        val secondText = second?.embeddingText
        
        // Then
        assertThat(firstText).isNotEqualTo(secondText) // 텍스트 변경됨
        assertThat(secondText).contains("Updated description")
        assertThat(schoolEmbeddingRepository.count()).isEqualTo(1L) // 여전히 1개
    }
    
    @Test
    fun `embedAllSchools 배치 작업이 정상 동작함`() {
        // Given: 5개 테스트 학교 생성
        val schools = (1..5).map { index ->
            School(
                name = "Test School Batch $index",
                type = "community_college",
                state = "CA",
                city = "Test City $index",
                tuition = 15000 + (index * 1000),
                description = "Test school number $index for batch embedding"
            )
        }
        schools.forEach { schoolRepository.save(it) }
        
        // When: 배치 임베딩 (시간 측정)
        val duration = measureTimeMillis {
            val successCount = embeddingService.embedAllSchools()
            assertThat(successCount).isGreaterThanOrEqualTo(5)
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
