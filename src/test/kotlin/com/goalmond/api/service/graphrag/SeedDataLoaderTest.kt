package com.goalmond.api.service.graphrag

import com.fasterxml.jackson.databind.ObjectMapper
import com.goalmond.api.domain.graphrag.EntityType
import com.goalmond.api.domain.graphrag.RelationType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.io.File

/**
 * Seed Data 삽입 테스트 (Phase 1 Week 2).
 * 
 * /media/ubuntu/data120g/college-crawler/data/seed_triples.json 파일을 읽어서
 * GraphRAG 테이블에 삽입합니다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("local")
@Import(GraphRagService::class)
class SeedDataLoaderTest {

    @Autowired
    private lateinit var graphRagService: GraphRagService

    private val logger = LoggerFactory.getLogger(SeedDataLoaderTest::class.java)
    private val objectMapper = ObjectMapper()

    data class SeedTriple(
        val head: String = "",
        val relation: String = "",
        val tail: String = "",
        val confidence: Double = 0.0
    )

    data class SchoolData(
        val school_name: String = "",
        val triples: List<SeedTriple> = emptyList()
    )

    data class SeedData(
        val metadata: Map<String, Any> = emptyMap(),
        val schools: List<SchoolData> = emptyList()
    )

    @Test
    fun `Seed Data를 DB에 삽입한다`() {
        // Given: seed_triples.json 파일 읽기
        val seedFile = File("/media/ubuntu/data120g/college-crawler/data/seed_triples.json")
        if (!seedFile.exists()) {
            logger.warn("⚠️  Seed Data 파일이 없습니다: ${seedFile.absolutePath}")
            return
        }

        val seedData = objectMapper.readValue(seedFile, SeedData::class.java)
        logger.info("📁 Seed Data 로드: ${seedData.schools.size}개 학교")

        var totalEntities = 0
        var totalTriples = 0

        // When: 각 학교별로 Entities와 Triples 생성
        for (schoolData in seedData.schools) {
            val schoolName = schoolData.school_name
            logger.info("🏫 처리 중: $schoolName (${schoolData.triples.size}개 Triples)")

            // Entity 맵 (중복 방지)
            val entityMap = mutableMapOf<String, java.util.UUID>()

            for (triple in schoolData.triples) {
                // Head Entity
                val headType = inferEntityType(triple.head, schoolName)
                val headEntity = graphRagService.findOrCreateEntity(
                    name = triple.head,
                    type = headType,
                    confidenceScore = triple.confidence
                )
                entityMap[triple.head] = headEntity.uuid!!

                // Tail Entity
                val tailType = inferEntityType(triple.tail, schoolName)
                val tailEntity = graphRagService.findOrCreateEntity(
                    name = triple.tail,
                    type = tailType,
                    confidenceScore = triple.confidence
                )
                entityMap[triple.tail] = tailEntity.uuid!!

                // Triple 생성
                val relationType = RelationType.valueOf(triple.relation)
                graphRagService.createTriple(
                    headEntity = headEntity,
                    relation = relationType,
                    tailEntity = tailEntity,
                    confidenceScore = triple.confidence,
                    sourceUrl = null
                )

                totalTriples++
            }

            totalEntities += entityMap.size
            logger.info("  ✅ {}: {} Entities, {} Triples", schoolName, entityMap.size, schoolData.triples.size)
        }

        // Then: 삽입 결과 검증
        logger.info("✨ Insert completed: {} schools, {} entities (before dedup), {} triples", 
            seedData.schools.size, totalEntities, totalTriples)

        assertThat(seedData.schools).hasSize(10)
        assertThat(totalTriples).isGreaterThanOrEqualTo(100) // 최소 100개 이상
    }

    /**
     * Entity 이름을 보고 타입을 추론합니다.
     */
    private fun inferEntityType(name: String, schoolName: String): EntityType {
        return when {
            name.contains("University") || name.contains("College") || name.contains("Institute") || name == schoolName -> EntityType.SCHOOL
            name.contains("Science") || name.contains("Engineering") || name.contains("Business") || name.contains("Art") -> EntityType.PROGRAM
            name.contains("Python") || name.contains("Java") || name.contains("Machine Learning") || name.contains("Statistics") -> EntityType.SKILL
            name.contains("Engineer") || name.contains("Developer") || name.contains("Manager") || name.contains("Scientist") || name.contains("Analyst") -> EntityType.JOB
            name.contains("Google") || name.contains("Microsoft") || name.contains("Amazon") || name.contains("Apple") || name.contains("Meta") || name.contains("Netflix") || name.contains("Tesla") -> EntityType.COMPANY
            else -> EntityType.LOCATION // Default
        }
    }
}
