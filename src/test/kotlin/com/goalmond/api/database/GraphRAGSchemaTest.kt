package com.goalmond.api.database

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql

/**
 * GraphRAG 스키마 테스트.
 *
 * V11 마이그레이션으로 생성된 entities, knowledge_triples 테이블의 구조를 검증합니다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("local")
@DisplayName("GraphRAG 스키마 테스트")
class GraphRAGSchemaTest {

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    @DisplayName("entities 테이블이 존재하는지 확인")
    fun `entities 테이블 존재 확인`() {
        val tableExists = jdbcTemplate.queryForObject(
            """
            SELECT EXISTS (
                SELECT FROM information_schema.tables 
                WHERE table_schema = 'public' 
                AND table_name = 'entities'
            )
            """.trimIndent(),
            Boolean::class.java
        ) ?: false
        assertThat(tableExists).isTrue()
    }

    @Test
    @DisplayName("knowledge_triples 테이블이 존재하는지 확인")
    fun `knowledge_triples 테이블 존재 확인`() {
        val tableExists = jdbcTemplate.queryForObject(
            """
            SELECT EXISTS (
                SELECT FROM information_schema.tables 
                WHERE table_schema = 'public' 
                AND table_name = 'knowledge_triples'
            )
            """.trimIndent(),
            Boolean::class.java
        ) ?: false
        assertThat(tableExists).isTrue()
    }

    @Test
    @DisplayName("entities 테이블 컬럼 확인")
    fun `entities 테이블 컬럼 확인`() {
        val columns = jdbcTemplate.queryForList(
            """
            SELECT column_name, data_type 
            FROM information_schema.columns 
            WHERE table_schema = 'public' 
            AND table_name = 'entities'
            ORDER BY ordinal_position
            """.trimIndent()
        )

        val columnNames = columns.map { (it["column_name"] as? String) ?: "" }.filter { it.isNotEmpty() }.toSet()
        
        assertThat(columnNames).contains(
            "uuid",
            "entity_type",
            "entity_name",
            "canonical_name",
            "aliases",
            "metadata",
            "school_id",
            "program_id",
            "confidence_score",
            "created_at",
            "updated_at"
        )
    }

    @Test
    @DisplayName("knowledge_triples 테이블 컬럼 확인")
    fun `knowledge_triples 테이블 컬럼 확인`() {
        val columns = jdbcTemplate.queryForList(
            """
            SELECT column_name, data_type 
            FROM information_schema.columns 
            WHERE table_schema = 'public' 
            AND table_name = 'knowledge_triples'
            ORDER BY ordinal_position
            """.trimIndent()
        )

        val columnNames = columns.map { (it["column_name"] as? String) ?: "" }.filter { it.isNotEmpty() }.toSet()
        
        assertThat(columnNames).contains(
            "id",
            "head_entity_uuid",
            "head_entity_type",
            "head_entity_name",
            "relation_type",
            "tail_entity_uuid",
            "tail_entity_type",
            "tail_entity_name",
            "weight",
            "confidence_score",
            "properties",
            "source_url",
            "created_at"
        )
    }

    @Test
    @DisplayName("entities 테이블 제약조건 확인")
    fun `entities 테이블 제약조건 확인`() {
        val constraints = jdbcTemplate.queryForList(
            """
            SELECT constraint_name, constraint_type
            FROM information_schema.table_constraints
            WHERE table_schema = 'public'
            AND table_name = 'entities'
            """.trimIndent()
        )

        val constraintNames = constraints.map { (it["constraint_name"] as? String) ?: "" }.filter { it.isNotEmpty() }.toSet()
        
        assertThat(constraintNames).containsAnyOf(
            "unique_entity",
            "valid_entity_type",
            "valid_reference"
        )
    }

    @Test
    @DisplayName("knowledge_triples 테이블 제약조건 확인")
    fun `knowledge_triples 테이블 제약조건 확인`() {
        val constraints = jdbcTemplate.queryForList(
            """
            SELECT constraint_name, constraint_type
            FROM information_schema.table_constraints
            WHERE table_schema = 'public'
            AND table_name = 'knowledge_triples'
            """.trimIndent()
        )

        val constraintNames = constraints.map { (it["constraint_name"] as? String) ?: "" }.filter { it.isNotEmpty() }.toSet()
        
        assertThat(constraintNames).containsAnyOf(
            "valid_relation_type",
            "valid_confidence",
            "valid_weight",
            "no_self_relation"
        )
    }

    @Test
    @DisplayName("entities 테이블 인덱스 확인")
    fun `entities 테이블 인덱스 확인`() {
        val indexes = jdbcTemplate.queryForList(
            """
            SELECT indexname
            FROM pg_indexes
            WHERE schemaname = 'public'
            AND tablename = 'entities'
            """.trimIndent()
        )

        val indexNames = indexes.map { (it["indexname"] as? String) ?: "" }.filter { it.isNotEmpty() }.toSet()
        
        assertThat(indexNames).containsAnyOf(
            "idx_entities_type_name",
            "idx_entities_school_id",
            "idx_entities_program_id",
            "idx_entities_aliases"
        )
    }

    @Test
    @DisplayName("knowledge_triples 테이블 인덱스 확인")
    fun `knowledge_triples 테이블 인덱스 확인`() {
        val indexes = jdbcTemplate.queryForList(
            """
            SELECT indexname
            FROM pg_indexes
            WHERE schemaname = 'public'
            AND tablename = 'knowledge_triples'
            """.trimIndent()
        )

        val indexNames = indexes.map { (it["indexname"] as? String) ?: "" }.filter { it.isNotEmpty() }.toSet()
        
        assertThat(indexNames).containsAnyOf(
            "idx_triples_head_relation",
            "idx_triples_tail_relation",
            "idx_triples_relation_type",
            "idx_triples_head_relation_tail"
        )
    }

    @Test
    @DisplayName("schools 테이블에 entity_uuid 컬럼 추가 확인")
    fun `schools 테이블 entity_uuid 컬럼 확인`() {
        val columns = jdbcTemplate.queryForList(
            """
            SELECT column_name
            FROM information_schema.columns
            WHERE table_schema = 'public'
            AND table_name = 'schools'
            AND column_name = 'entity_uuid'
            """.trimIndent()
        )

        assertThat(columns).isNotEmpty()
    }

    @Test
    @DisplayName("programs 테이블에 entity_uuid 컬럼 추가 확인")
    fun `programs 테이블 entity_uuid 컬럼 확인`() {
        val columns = jdbcTemplate.queryForList(
            """
            SELECT column_name
            FROM information_schema.columns
            WHERE table_schema = 'public'
            AND table_name = 'programs'
            AND column_name = 'entity_uuid'
            """.trimIndent()
        )

        assertThat(columns).isNotEmpty()
    }
}
