package com.goalmond.api.service.graphrag

import com.goalmond.api.domain.graphrag.EntityType
import com.goalmond.api.domain.graphrag.RelationType
import com.goalmond.api.repository.GraphRagEntityRepository
import com.goalmond.api.repository.KnowledgeTripleRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

/**
 * GraphSearchService 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class GraphSearchServiceTest {
    
    @Autowired
    private lateinit var graphSearchService: GraphSearchService
    
    @Autowired
    private lateinit var graphRagService: GraphRagService
    
    @Autowired
    private lateinit var entityRepository: GraphRagEntityRepository
    
    @Autowired
    private lateinit var tripleRepository: KnowledgeTripleRepository
    
    @BeforeEach
    fun setup() {
        // Test data setup
        // Path: Stanford -> CS -> ML -> AI Engineer -> Google
        val stanford = graphRagService.findOrCreateEntity("Stanford University", EntityType.SCHOOL)
        val csProgram = graphRagService.findOrCreateEntity("Computer Science", EntityType.PROGRAM)
        val mlSkill = graphRagService.findOrCreateEntity("Machine Learning", EntityType.SKILL)
        val aiJob = graphRagService.findOrCreateEntity("AI Engineer", EntityType.JOB)
        val google = graphRagService.findOrCreateEntity("Google", EntityType.COMPANY)
        
        // Create triples
        graphRagService.createTriple(stanford, RelationType.OFFERS, csProgram, weight = 1.0, confidenceScore = 0.95)
        graphRagService.createTriple(csProgram, RelationType.DEVELOPS, mlSkill, weight = 1.0, confidenceScore = 0.9)
        graphRagService.createTriple(mlSkill, RelationType.LEADS_TO, aiJob, weight = 0.8, confidenceScore = 0.85)
        graphRagService.createTriple(google, RelationType.HIRES_FROM, aiJob, weight = 0.9, confidenceScore = 0.9)
    }
    
    @Test
    fun `역추적 Career Path 테스트`() {
        // When
        val paths = graphSearchService.findCareerPaths(
            targetCompany = "Google",
            targetJob = "AI Engineer",
            requiredSkills = listOf("Machine Learning")
        )
        
        // Then
        assertThat(paths).isNotEmpty
        assertThat(paths[0].schoolName).contains("Stanford")
        assertThat(paths[0].skills).contains("Machine Learning")
        assertThat(paths[0].company).isEqualTo("Google")
        assertThat(paths[0].weight).isGreaterThan(0.4)  // 4-hop 누적 곱: 0.9×0.9 × 0.8×0.85 × 1.0×0.9 × 1.0×0.95 ≈ 0.471
    }
    
    @Test
    fun `Skill 기반 Program 검색 테스트`() {
        // When
        val results = graphSearchService.findProgramsBySkills(
            skills = listOf("Machine Learning", "Deep Learning")
        )
        
        // Then
        assertThat(results).isNotEmpty
        assertThat(results[0].programName).isNotBlank()
        assertThat(results[0].matchedSkills).isNotEmpty
        assertThat(results[0].relevanceScore).isGreaterThan(0.0)
    }
    
    @Test
    fun `1-hop 이웃 검색 테스트`() {
        // Given
        val stanford = entityRepository.findByEntityTypeAndCanonicalName(
            EntityType.SCHOOL,
            "stanford university"
        ).get()
        
        // When
        val neighbors = graphSearchService.findNeighbors(stanford.uuid!!, hops = 1)
        
        // Then
        assertThat(neighbors).isNotEmpty
        assertThat(neighbors).anyMatch { neighborId ->
            val neighbor = entityRepository.findById(neighborId).orElse(null)
            neighbor?.entityType == EntityType.PROGRAM
        }
    }
    
    @Test
    fun `2-hop 이웃 검색 테스트`() {
        // Given
        val stanford = entityRepository.findByEntityTypeAndCanonicalName(
            EntityType.SCHOOL,
            "stanford university"
        ).get()
        
        // When
        val neighbors = graphSearchService.findNeighbors(stanford.uuid!!, hops = 2)
        
        // Then
        assertThat(neighbors).isNotEmpty
        assertThat(neighbors.size).isGreaterThan(1)  // 2-hop이므로 더 많은 결과
    }
    
    @Test
    fun `존재하지 않는 Company 검색 시 빈 리스트 반환`() {
        // When
        val paths = graphSearchService.findCareerPaths(
            targetCompany = "NonExistentCompany"
        )
        
        // Then
        assertThat(paths).isEmpty()
    }
    
    @Test
    fun `빈 Skill 리스트로 검색 시 빈 리스트 반환`() {
        // When
        val results = graphSearchService.findProgramsBySkills(skills = emptyList())
        
        // Then
        assertThat(results).isEmpty()
    }
}
