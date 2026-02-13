package com.goalmond.api.service.graphrag

import com.goalmond.api.domain.graphrag.EntityType
import com.goalmond.api.repository.GraphRagEntityRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

/**
 * EntityResolutionService 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EntityResolutionServiceTest {
    
    @Autowired
    private lateinit var entityResolutionService: EntityResolutionService
    
    @Autowired
    private lateinit var entityRepository: GraphRagEntityRepository
    
    @Autowired
    private lateinit var graphRagService: GraphRagService
    
    @Test
    fun `Canonical name 정규화 테스트`() {
        // Given
        val inputs = listOf(
            "Google Inc." to "google",
            "Microsoft  Corporation" to "microsoft",
            "Amazon & Associates" to "amazon and associates",
            "  Apple  LLC  " to "apple"
        )
        
        // When & Then
        inputs.forEach { (input, expected) ->
            val result = entityResolutionService.normalizeEntityName(input)
            assertThat(result).isEqualTo(expected)
        }
    }
    
    @Test
    fun `Alias 매칭 테스트`() {
        // Given
        val entity = graphRagService.findOrCreateEntity(
            name = "Google",
            type = EntityType.COMPANY,
            aliases = listOf("Google Inc", "Google LLC", "Alphabet")
        )
        
        // When
        val result1 = entityResolutionService.findByAlias("Google Inc.", EntityType.COMPANY)
        val result2 = entityResolutionService.findByAlias("Alphabet", EntityType.COMPANY)
        val result3 = entityResolutionService.findByAlias("Microsoft", EntityType.COMPANY)
        
        // Then
        assertThat(result1).isNotNull
        assertThat(result1?.uuid).isEqualTo(entity.uuid)
        assertThat(result2).isNotNull
        assertThat(result2?.uuid).isEqualTo(entity.uuid)
        assertThat(result3).isNull()
    }
    
    @Test
    fun `유사도 점수 계산 테스트`() {
        // Given
        val entity1 = graphRagService.findOrCreateEntity("Stanford University", EntityType.SCHOOL)
        val entity2 = graphRagService.findOrCreateEntity("Stanford University", EntityType.SCHOOL)
        val entity3 = graphRagService.findOrCreateEntity(
            "Stanford",
            EntityType.SCHOOL,
            aliases = listOf("Stanford University")
        )
        val entity4 = graphRagService.findOrCreateEntity("Harvard University", EntityType.SCHOOL)
        
        // When
        val score1 = entityResolutionService.calculateSimilarityScore(entity1, entity2)
        val score2 = entityResolutionService.calculateSimilarityScore(entity1, entity3)
        val score3 = entityResolutionService.calculateSimilarityScore(entity1, entity4)
        
        // Then
        assertThat(score1).isEqualTo(1.0)  // Exact match
        assertThat(score2).isGreaterThanOrEqualTo(0.9)  // Alias match
        assertThat(score3).isLessThan(0.5)  // Different entities
    }
    
    @Test
    fun `중복 Entity 감지 테스트`() {
        // Given
        graphRagService.findOrCreateEntity("Stanford University", EntityType.SCHOOL)
        graphRagService.findOrCreateEntity("Stanford Univ", EntityType.SCHOOL)
        graphRagService.findOrCreateEntity("MIT", EntityType.SCHOOL)
        
        // When
        val duplicates = entityResolutionService.findDuplicateCandidates(EntityType.SCHOOL, 0.8)
        
        // Then
        assertThat(duplicates).isNotEmpty
        assertThat(duplicates).anyMatch { (e1, e2) ->
            e1.entityName.contains("Stanford") && e2.entityName.contains("Stanford")
        }
    }
    
    @Test
    fun `신뢰도 점수 재계산 테스트`() {
        // Given
        val entity = graphRagService.findOrCreateEntity(
            name = "Google",
            type = EntityType.COMPANY,
            confidenceScore = 0.8,
            sourceUrls = arrayOf("https://google.com", "https://careers.google.com")
        )
        
        // When
        val newScore = entityResolutionService.recalculateConfidenceScore(entity, tripleCount = 10)
        
        // Then
        assertThat(newScore).isGreaterThan(0.8)  // Source + Triple 보너스
        assertThat(newScore).isLessThanOrEqualTo(1.0)  // 최대값 제한
    }
}
