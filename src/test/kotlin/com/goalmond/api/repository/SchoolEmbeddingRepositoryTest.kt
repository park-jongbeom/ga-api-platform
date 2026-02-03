package com.goalmond.api.repository

import com.goalmond.api.domain.entity.School
import com.goalmond.api.domain.entity.SchoolEmbedding
import com.goalmond.api.repository.ProgramRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import jakarta.persistence.EntityManager
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * SchoolEmbeddingRepository 통합 테스트 (GAM-3, Phase 0).
 * 
 * 테스트 목표:
 * 1. pgvector extension 설치 확인
 * 2. school_embeddings 테이블 생성 확인
 * 3. 768차원 벡터 INSERT 성공
 * 4. 코사인 유사도 검색 쿼리 실행 성공
 * 5. IVFFlat 인덱스 생성 확인
 * 
 * 주의: pgvector는 PostgreSQL extension이므로 H2 in-memory DB 사용 불가.
 * 실제 PostgreSQL DB 또는 Testcontainers 필요.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("local")
class SchoolEmbeddingRepositoryTest {
    
    @Autowired
    private lateinit var schoolEmbeddingRepository: SchoolEmbeddingRepository
    
    @Autowired
    private lateinit var schoolRepository: SchoolRepository
    
    @Autowired
    private lateinit var programRepository: ProgramRepository
    
    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate
    
    @Autowired
    private lateinit var entityManager: EntityManager
    
    private lateinit var testSchool: School
    private val testPrefix = "[TEST_EMBED] "
    
    @BeforeEach
    fun setUp() {
        // 테스트 데이터 정리 (실제 DB 사용하므로 안전한 범위만 삭제)
        val testSchools = schoolRepository.findByNameStartingWith(testPrefix)
        testSchools.forEach { school ->
            programRepository.findBySchoolId(school.id!!).let { programs ->
                if (programs.isNotEmpty()) {
                    programRepository.deleteAll(programs)
                }
            }
            schoolEmbeddingRepository.findBySchoolId(school.id!!)?.let {
                schoolEmbeddingRepository.delete(it)
            }
            schoolRepository.delete(school)
        }
        
        // 테스트용 School 생성
        testSchool = School(
            name = "${testPrefix}Irvine Valley College",
            type = "community_college",
            state = "CA",
            city = "Irvine",
            tuition = 18000,
            livingCost = 15000,
            description = "Premier community college in Orange County"
        )
        testSchool = schoolRepository.save(testSchool)
    }
    
    @Test
    fun `pgvector extension이 설치되어 있다`() {
        val result = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM pg_extension WHERE extname = 'vector'",
            Int::class.java
        )
        
        assertThat(result).isGreaterThan(0)
    }
    
    @Test
    fun `school_embeddings 테이블이 존재한다`() {
        val result = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*) FROM information_schema.tables 
            WHERE table_name = 'school_embeddings'
            """,
            Int::class.java
        )
        
        assertThat(result).isEqualTo(1)
    }
    
    @Test
    fun `768차원 벡터를 INSERT할 수 있다`() {
        // Given: 768차원 랜덤 벡터 생성
        val embedding768 = List(768) { Math.random() }
        val embeddingStr = embedding768.joinToString(prefix = "[", postfix = "]")
        
        // When: Native Query로 SchoolEmbedding 저장
        schoolEmbeddingRepository.insertOrUpdateEmbedding(
            schoolId = testSchool.id!!,
            embeddingText = "Test school in California with good reputation",
            embeddingVector = embeddingStr
        )
        
        // Then: 저장 확인
        val saved = schoolEmbeddingRepository.findBySchoolId(testSchool.id!!)
        assertThat(saved).isNotNull
        assertThat(saved?.schoolId).isEqualTo(testSchool.id)
        assertThat(saved?.embeddingText).isEqualTo("Test school in California with good reputation")
        
        // 벡터 파싱 검증
        val parsed = saved?.getEmbeddingVector()
        assertThat(parsed).hasSize(768)
    }
    
    @Test
    fun `코사인 유사도 검색 쿼리가 실행된다`() {
        // Given: 3개 학교 임베딩 저장
        val schools = listOf(
            createAndSaveSchool("School A", listOf(0.1, 0.2, 0.3)),
            createAndSaveSchool("School B", listOf(0.2, 0.3, 0.4)),
            createAndSaveSchool("School C", listOf(0.9, 0.8, 0.7))
        )
        
        // When: School A와 유사한 학교 검색 (Top 2)
        val queryVector = List(768) { 
            when (it) {
                0 -> 0.1
                1 -> 0.2
                2 -> 0.3
                else -> 0.0
            }
        }
        val queryEmbedding = queryVector.joinToString(prefix = "[", postfix = "]")
        
        val results = schoolEmbeddingRepository.findTopByCosineSimilarity(queryEmbedding, 2)
        
        // Then
        assertThat(results).hasSize(2)
        assertThat(results[0].getSimilarity()).isGreaterThan(results[1].getSimilarity())
        
        // 유사도 범위 검증 (0.0 ~ 1.0)
        results.forEach { result ->
            assertThat(result.getSimilarity()).isBetween(-1.0, 1.0)
        }
    }
    
    @Test
    fun `IVFFlat 인덱스가 생성되어 있다`() {
        val result = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*) FROM pg_indexes 
            WHERE tablename = 'school_embeddings' 
            AND indexname = 'school_embeddings_embedding_idx'
            """,
            Int::class.java
        )
        
        assertThat(result).isEqualTo(1)
    }
    
    @Test
    @Transactional
    fun `school_id UNIQUE 제약조건이 동작한다 - UPSERT로 업데이트됨`() {
        // Given: 첫 번째 임베딩 저장
        schoolEmbeddingRepository.insertOrUpdateEmbedding(
            schoolId = testSchool.id!!,
            embeddingText = "First embedding",
            embeddingVector = List(768) { 0.1 }.joinToString(prefix = "[", postfix = "]")
        )
        entityManager.flush()
        entityManager.clear()
        
        val first = schoolEmbeddingRepository.findBySchoolId(testSchool.id!!)
        assertThat(first?.embeddingText).isEqualTo("First embedding")
        
        // When: 같은 school_id로 두 번째 저장 시도 (UPSERT)
        schoolEmbeddingRepository.insertOrUpdateEmbedding(
            schoolId = testSchool.id!!,
            embeddingText = "Second embedding",
            embeddingVector = List(768) { 0.2 }.joinToString(prefix = "[", postfix = "]")
        )
        entityManager.flush()
        entityManager.clear()
        
        // Then: 업데이트되어 기존 데이터가 변경됨
        val second = schoolEmbeddingRepository.findBySchoolId(testSchool.id!!)
        assertThat(second?.embeddingText).isEqualTo("Second embedding")
        
        // 전체 개수는 여전히 1개
        assertThat(schoolEmbeddingRepository.countBySchoolId(testSchool.id!!)).isEqualTo(1L)
    }
    
    @Test
    fun `findBySchoolId로 임베딩을 조회할 수 있다`() {
        // Given
        schoolEmbeddingRepository.insertOrUpdateEmbedding(
            schoolId = testSchool.id!!,
            embeddingText = "Test embedding",
            embeddingVector = List(768) { 0.5 }.joinToString(prefix = "[", postfix = "]")
        )
        
        // When
        val found = schoolEmbeddingRepository.findBySchoolId(testSchool.id!!)
        
        // Then
        assertThat(found).isNotNull
        assertThat(found?.schoolId).isEqualTo(testSchool.id)
        assertThat(found?.embeddingText).isEqualTo("Test embedding")
    }
    
    // Helper 함수
    private fun createAndSaveSchool(name: String, vectorPrefix: List<Double>): School {
        val school = School(
            name = "$testPrefix$name",
            type = "community_college",
            state = "CA",
            city = "Test City",
            tuition = 20000
        )
        val saved = schoolRepository.save(school)
        
        val embedding = List(768) { i ->
            if (i < vectorPrefix.size) vectorPrefix[i] else 0.0
        }
        val embeddingStr = embedding.joinToString(prefix = "[", postfix = "]")
        
        schoolEmbeddingRepository.insertOrUpdateEmbedding(
            schoolId = saved.id!!,
            embeddingText = "Embedding for $name",
            embeddingVector = embeddingStr
        )
        
        return saved
    }
}
