package com.goalmond.api.support

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.PostgreSQLContainer

/**
 * Testcontainers 기반 PostgreSQL 설정.
 *
 * 전체 테스트 수트 실행 시 AWS RDS 연결 한계 초과(FATAL: remaining connection slots)를 방지하기 위해
 * EmbeddingServiceTest, VectorSearchServiceTest, MatchingEngineServiceTest에서 사용.
 *
 * pgvector/pgvector:pg16 이미지는 vector extension이 사전 설치되어 있어 V4 migration 실행 가능.
 */
@TestConfiguration(proxyBeanMethods = false)
class PostgresTestcontainersConfig {

    @Bean
    @ServiceConnection
    fun postgresContainer(): PostgreSQLContainer<*> =
        PostgreSQLContainer("pgvector/pgvector:pg16")
}
