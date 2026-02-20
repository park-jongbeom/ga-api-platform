plugins {
    kotlin("jvm") version "1.9.24"
    kotlin("plugin.spring") version "1.9.24"
    id("org.springframework.boot") version "3.4.0"
    id("io.spring.dependency-management") version "1.1.5"
    jacoco
}

group = "com.goalmond"
version = "1.0.0-SNAPSHOT"

// Spring AI 버전 설정
extra["springAiVersion"] = "1.0.0-M4"

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }  // Spring AI Milestone
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "21"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

jacoco {
    toolVersion = "0.8.11"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    classDirectories.setFrom(
        sourceSets.main.get().output.classesDirs.files.map { dir ->
            fileTree(dir) {
                exclude(
                    "**/domain/dto/**",
                    "**/domain/entity/**",
                    "**/ApiApplication.class",
                    "**/config/TestUser*.class"
                )
            }
        }
    )
}

// 목표: 비즈니스 로직(service, controller, config) 테스트 커버리지 80% 이상.
// 리포트: ./gradlew test jacocoTestReport → build/reports/jacoco/test/html/index.html
// DTO/Entity/Application 제외 후 측정.

dependencies {
    // Spring Boot Web
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.5")

    // Database
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // Jackson Kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    
    // Spring AI (무료 Gemini API용 - Vertex AI 제외)
    implementation(platform("org.springframework.ai:spring-ai-bom:${property("springAiVersion")}"))
    implementation("org.springframework.ai:spring-ai-core")
    implementation("org.springframework.ai:spring-ai-pgvector-store")

    // Google GenAI 공식 Java SDK (Gemini API - generateContent, embedContent 등)
    // 공식 문서 Maven 예시 → Gradle: implementation("com.google.genai:google-genai:버전")
    implementation("com.google.genai:google-genai:1.37.0")
    
    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.slf4j", module = "slf4j-simple")
    }
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("io.mockk:mockk:1.13.10")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.mockito:mockito-inline:5.2.0")

    // Testcontainers (로컬 Docker PostgreSQL - RDS 연결 한계 초과 방지)
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:junit-jupiter")
}
