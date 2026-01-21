plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    // 프로젝트 모듈
    implementation(project(":ga-common"))
    implementation(project(":ga-grpc-interface"))
    
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    
    // Micrometer (Performance Monitoring)
    implementation("io.micrometer:micrometer-core")
    implementation("io.micrometer:micrometer-registry-prometheus")
    
    // AOP for Performance Monitoring
    implementation("org.springframework.boot:spring-boot-starter-aop")
    
    // LangChain4j - AI/LLM 통합
    implementation("dev.langchain4j:langchain4j:0.36.2")
    implementation("dev.langchain4j:langchain4j-open-ai:0.36.2")
    implementation("dev.langchain4j:langchain4j-embeddings:0.36.2")
    implementation("dev.langchain4j:langchain4j-pgvector:0.36.2")
    
    // Rate Limiting - Bucket4j
    implementation("com.bucket4j:bucket4j-core:8.10.1")
    implementation("com.bucket4j:bucket4j-redis:8.10.1")
    
    // gRPC Client
    implementation("io.grpc:grpc-stub:1.60.1")
    implementation("io.grpc:grpc-netty-shaded:1.60.1")
    
    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.5")
    implementation("io.jsonwebtoken:jjwt-impl:0.12.5")
    implementation("io.jsonwebtoken:jjwt-jackson:0.12.5")
    
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    
    // Database
    runtimeOnly("org.postgresql:postgresql")
    
    // Jackson
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    
    // SpringDoc OpenAPI (Swagger)
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")
    
    // OWASP Java Encoder - XSS 방어
    implementation("org.owasp.encoder:encoder:1.2.3")
    
    // Flyway - DB 마이그레이션
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    
    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.slf4j", module = "slf4j-simple")
    }
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("io.mockk:mockk:1.13.10")
    testImplementation("org.testcontainers:testcontainers:1.19.3")
    testImplementation("org.testcontainers:postgresql:1.19.3")
    testImplementation("org.testcontainers:junit-jupiter:1.19.3")
    testImplementation("com.h2database:h2")
    testImplementation("it.ozimov:embedded-redis:0.7.3") {
        exclude(group = "org.slf4j", module = "slf4j-simple")
    }
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
}
