package com.goalmond.api.integration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local", "test")
@Testcontainers
@DisplayName("Auth & User Profile 통합 테스트 (실제 DB)")
class AuthAndUserProfileIntegrationTest {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    private val objectMapper = jacksonObjectMapper()

    companion object {
        @Container
        @JvmStatic
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer(DockerImageName.parse("postgres:17"))
            .withDatabaseName("ga_test")
            .withUsername("test")
            .withPassword("test")

        @DynamicPropertySource
        @JvmStatic
        fun configureDataSource(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.datasource.driver-class-name", { "org.postgresql.Driver" })
        }
    }

    private fun uniqueEmail() = "integration-${java.util.UUID.randomUUID().toString().take(8)}@example.com"

    @Test
    @DisplayName("회원가입 -> 로그인 -> 토큰으로 프로필/학력/유학목표 저장")
    fun signupLoginThenSaveProfileEducationPreference() {
        val email = uniqueEmail()
        val password = "password123"
        val signupBody = mapOf("email" to email, "password" to password)
        val signupRes = restTemplate.postForEntity("/api/v1/auth/signup", signupBody, String::class.java)
        assertEquals(HttpStatus.OK, signupRes.statusCode)
        val signupJson = objectMapper.readValue<Map<String, Any>>(signupRes.body!!)
        @Suppress("UNCHECKED_CAST")
        val signupData = signupJson["data"] as Map<String, Any>
        val token = signupData["token"] as String
        assertNotNull(token)
        assertTrue(token.isNotBlank())

        val loginBody = mapOf("email" to email, "password" to password)
        val loginRes = restTemplate.postForEntity("/api/v1/auth/login", loginBody, String::class.java)
        assertEquals(HttpStatus.OK, loginRes.statusCode)

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            set("Authorization", "Bearer $token")
        }
        val profileBody = mapOf("mbti" to "INTJ", "tags" to "체계적,논리적", "bio" to "통합 테스트용")
        val profileRes = restTemplate.exchange(
            "/api/v1/user/profile", HttpMethod.PUT, HttpEntity(profileBody, headers), String::class.java
        )
        assertEquals(HttpStatus.OK, profileRes.statusCode)

        val educationBody = mapOf(
            "schoolName" to "테스트고등학교", "schoolLocation" to "서울", "gpa" to 3.5,
            "englishTestType" to "TOEFL", "englishScore" to 95, "degree" to "고등학교"
        )
        val educationRes = restTemplate.exchange(
            "/api/v1/user/education", HttpMethod.POST, HttpEntity(educationBody, headers), String::class.java
        )
        assertEquals(HttpStatus.OK, educationRes.statusCode)

        val preferenceBody = mapOf("targetProgram" to "community_college", "budgetUsd" to 50000)
        val preferenceRes = restTemplate.exchange(
            "/api/v1/user/preference", HttpMethod.POST, HttpEntity(preferenceBody, headers), String::class.java
        )
        assertEquals(HttpStatus.OK, preferenceRes.statusCode)
    }

    @Test
    @DisplayName("중복 이메일 회원가입 시 400")
    fun signupDuplicateEmailReturns400() {
        val email = uniqueEmail()
        val body = mapOf("email" to email, "password" to "password123")
        restTemplate.postForEntity("/api/v1/auth/signup", body, String::class.java)
        val second = restTemplate.postForEntity("/api/v1/auth/signup", body, String::class.java)
        assertEquals(HttpStatus.BAD_REQUEST, second.statusCode)
    }

    @Test
    @DisplayName("잘못된 비밀번호 로그인 시 401")
    fun loginWrongPasswordReturns401() {
        val email = uniqueEmail()
        restTemplate.postForEntity("/api/v1/auth/signup", mapOf("email" to email, "password" to "password123"), String::class.java)
        val loginRes = restTemplate.postForEntity("/api/v1/auth/login", mapOf("email" to email, "password" to "wrong"), String::class.java)
        assertEquals(HttpStatus.UNAUTHORIZED, loginRes.statusCode)
    }

    @Test
    @DisplayName("토큰 없이 User Profile API 호출 시 401")
    fun userProfileWithoutTokenReturns401() {
        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
        val res = restTemplate.exchange("/api/v1/user/profile", HttpMethod.PUT, HttpEntity(mapOf("mbti" to "INTJ"), headers), String::class.java)
        assertEquals(HttpStatus.UNAUTHORIZED, res.statusCode)
    }
}
