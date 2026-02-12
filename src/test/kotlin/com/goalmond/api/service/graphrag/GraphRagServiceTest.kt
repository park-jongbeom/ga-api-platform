package com.goalmond.api.service.graphrag

import com.goalmond.api.domain.graphrag.EntityType
import com.goalmond.api.domain.graphrag.RelationType
import com.goalmond.api.repository.GraphRagEntityRepository
import com.goalmond.api.repository.KnowledgeTripleRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

/**
 * GraphRAG Service 통합 테스트 (Phase 1).
 * 
 * 테스트 목표:
 * 1. entities 테이블 생성 확인
 * 2. knowledge_triples 테이블 생성 확인
 * 3. findOrCreateEntity() 동작 검증
 * 4. createTriple() 동작 검증
 * 5. 중복 Entity/Triple 처리 검증
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("local")
@Import(GraphRagService::class)
class GraphRagServiceTest {

    @Autowired
    private lateinit var graphRagService: GraphRagService

    @Autowired
    private lateinit var entityRepository: GraphRagEntityRepository

    @Autowired
    private lateinit var tripleRepository: KnowledgeTripleRepository

    @Test
    fun `entities 테이블이 정상적으로 생성되었는지 확인`() {
        // Given: School Entity 생성
        val school = graphRagService.findOrCreateEntity(
            name = "Stanford University",
            type = EntityType.SCHOOL,
            aliases = listOf("Stanford", "스탠포드"),
            confidenceScore = 0.95
        )

        // Then: Entity가 정상적으로 저장됨
        assertThat(school).isNotNull
        assertThat(school.uuid).isNotNull()
        assertThat(school.entityName).isEqualTo("Stanford University")
        assertThat(school.entityType).isEqualTo(EntityType.SCHOOL)
        assertThat(school.canonicalName).isEqualTo("stanford university")
    }

    @Test
    fun `findOrCreateEntity는 동일한 Entity를 중복 생성하지 않음`() {
        // Given: 첫 번째 Entity 생성
        val entity1 = graphRagService.findOrCreateEntity(
            name = "Computer Science",
            type = EntityType.PROGRAM
        )

        // When: 동일한 이름으로 다시 호출
        val entity2 = graphRagService.findOrCreateEntity(
            name = "Computer Science",
            type = EntityType.PROGRAM
        )

        // Then: 동일한 UUID를 반환
        assertThat(entity1.uuid).isEqualTo(entity2.uuid)
        
        // And: DB에 1개만 존재
        val all = entityRepository.findByEntityType(EntityType.PROGRAM)
        val csEntities = all.filter { it.canonicalName == "computer science" }
        assertThat(csEntities).hasSize(1)
    }

    @Test
    fun `knowledge_triples 테이블이 정상적으로 생성되었는지 확인`() {
        // Given: School과 Program Entity 생성
        val school = graphRagService.findOrCreateEntity(
            name = "MIT",
            type = EntityType.SCHOOL
        )
        val program = graphRagService.findOrCreateEntity(
            name = "Artificial Intelligence",
            type = EntityType.PROGRAM
        )

        // When: Triple 생성 (MIT OFFERS AI Program)
        val triple = graphRagService.createTriple(
            headEntity = school,
            relation = RelationType.OFFERS,
            tailEntity = program,
            confidenceScore = 0.9
        )

        // Then: Triple이 정상적으로 저장됨
        assertThat(triple).isNotNull
        assertThat(triple?.id).isNotNull()
        assertThat(triple?.headEntityName).isEqualTo("MIT")
        assertThat(triple?.relationType).isEqualTo(RelationType.OFFERS)
        assertThat(triple?.tailEntityName).isEqualTo("Artificial Intelligence")
    }

    @Test
    fun `createTriple은 중복 Triple을 생성하지 않음`() {
        // Given: Entities 생성
        val school = graphRagService.findOrCreateEntity("UC Berkeley", EntityType.SCHOOL)
        val location = graphRagService.findOrCreateEntity("California", EntityType.LOCATION)

        // When: 동일한 Triple을 두 번 생성
        val triple1 = graphRagService.createTriple(school, RelationType.LOCATED_IN, location)
        val triple2 = graphRagService.createTriple(school, RelationType.LOCATED_IN, location)

        // Then: 동일한 ID 반환
        assertThat(triple1?.id).isEqualTo(triple2?.id)
        
        // And: DB에 1개만 존재
        val triples = tripleRepository.findByHeadEntityUuidAndRelationType(
            school.uuid!!,
            RelationType.LOCATED_IN
        )
        assertThat(triples).hasSize(1)
    }

    @Test
    fun `다양한 Entity 타입과 Relation 타입을 생성할 수 있음`() {
        // Given: 다양한 Entity 생성
        val school = graphRagService.findOrCreateEntity("Harvard", EntityType.SCHOOL)
        val program = graphRagService.findOrCreateEntity("Business Administration", EntityType.PROGRAM)
        val skill = graphRagService.findOrCreateEntity("Leadership", EntityType.SKILL)
        val job = graphRagService.findOrCreateEntity("CEO", EntityType.JOB)

        // When: 다양한 Triple 생성
        graphRagService.createTriple(school, RelationType.OFFERS, program)
        graphRagService.createTriple(program, RelationType.DEVELOPS, skill)
        graphRagService.createTriple(job, RelationType.REQUIRES, skill)

        // Then: 모든 Entity와 Triple이 저장됨
        assertThat(entityRepository.count()).isGreaterThanOrEqualTo(4)
        assertThat(tripleRepository.count()).isGreaterThanOrEqualTo(3)
    }
}
