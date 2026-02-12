package com.goalmond.api.config.ai

import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.vectorstore.PgVectorStore
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import javax.sql.DataSource

/**
 * Spring AI PGVectorStore 설정.
 * 
 * 학교 문서와 프로그램 문서를 위한 별도의 VectorStore를 생성합니다.
 * 기존 pgvector 테이블(school_documents, program_documents)을 활용합니다.
 */
@Configuration
class VectorStoreConfig {
    
    /**
     * 학교 문서용 VectorStore.
     * 
     * school_documents 테이블의 embedding 컬럼과 연동합니다.
     * Spring AI가 자동으로 코사인 유사도 검색을 수행합니다.
     */
    @Bean("schoolDocumentVectorStore")
    fun schoolDocumentVectorStore(
        dataSource: DataSource,
        embeddingModel: CustomGeminiEmbeddingClient
    ): VectorStore {
        val jdbcTemplate = JdbcTemplate(dataSource)
        return PgVectorStore.Builder(jdbcTemplate, embeddingModel)
            .withDimensions(768)
            .withVectorTableName("school_documents")
            .withSchemaName("public")
            .withDistanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
            .withRemoveExistingVectorStoreTable(false)
            .withInitializeSchema(false)
            .build()
    }
    
    /**
     * 프로그램 문서용 VectorStore.
     * 
     * program_documents 테이블의 embedding 컬럼과 연동합니다.
     */
    @Bean("programDocumentVectorStore")
    fun programDocumentVectorStore(
        dataSource: DataSource,
        embeddingModel: CustomGeminiEmbeddingClient
    ): VectorStore {
        val jdbcTemplate = JdbcTemplate(dataSource)
        return PgVectorStore.Builder(jdbcTemplate, embeddingModel)
            .withDimensions(768)
            .withVectorTableName("program_documents")
            .withSchemaName("public")
            .withDistanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
            .withRemoveExistingVectorStoreTable(false)
            .withInitializeSchema(false)
            .build()
    }
}
