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
        val jsonHeaders = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
        val signupBody = """{"email":"$email","password":"$password"}"""
        val signupRes = restTemplate.exchange(
            "/api/v1/auth/signup",
            HttpMethod.POST,
            HttpEntity(signupBody, jsonHeaders),
            String::class.java
        )
        assertEquals(HttpStatus.OK, signupRes.statusCode, "signup: ${signupRes.body}")
        val signupJson = objectMapper.readValue<Map<String, Any>>(signupRes.body!!)
        @Suppress("UNCHECKED_CAST")
        val signupData = signupJson["data"] as Map<String, Any>
        val token = signupData["token"] as String
        assertNotNull(token)
        assertTrue(token.isNotBlank())

        val loginBody = """{"email":"$email","password":"$password"}"""
        val loginRes = restTemplate.exchange(
            "/api/v1/auth/login",
            HttpMethod.POST,
            HttpEntity(loginBody, jsonHeaders),
            String::class.java
        )
        assertEquals(HttpStatus.OK, loginRes.statusCode, "login: ${loginRes.body}")

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            set("Authorization", "Bearer $token")
        }
        val profileBody = """{"mbti":"INTJ","tags":"체계적,논리적","bio":"통합 테스트용"}"""
        val profileRes = restTemplate.exchange(
            "/api/v1/user/profile", HttpMethod.PUT, HttpEntity(profileBody, headers), String::class.java
        )
        assertEquals(HttpStatus.OK, profileRes.statusCode, "profile: ${profileRes.body}")

        val educationBody = """{"school_name":"테스트고등학교","school_location":"서울","gpa":3.5,"english_test_type":"TOEFL","english_score":95,"degree":"고등학교"}"""
        val educationRes = restTemplate.exchange(
            "/api/v1/user/education", HttpMethod.POST, HttpEntity(educationBody, headers), String::class.java
        )
        assertEquals(HttpStatus.OK, educationRes.statusCode, "education: ${educationRes.body}")

        val preferenceBody = """{"target_program":"community_college","budget_usd":50000}"""
        val preferenceRes = restTemplate.exchange(
            "/api/v1/user/preference", HttpMethod.POST, HttpEntity(preferenceBody, headers), String::class.java
        )
        assertEquals(HttpStatus.OK, preferenceRes.statusCode, "preference: ${preferenceRes.body}")
    }

    @Test
    @DisplayName("중복 이메일 회원가입 시 400")
    fun signupDuplicateEmailReturns400() {
        val email = uniqueEmail()
        val body = """{"email":"$email","password":"password123"}"""
        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
        restTemplate.exchange("/api/v1/auth/signup", HttpMethod.POST, HttpEntity(body, headers), String::class.java)
        val second = restTemplate.exchange("/api/v1/auth/signup", HttpMethod.POST, HttpEntity(body, headers), String::class.java)
        assertEquals(HttpStatus.BAD_REQUEST, second.statusCode)
    }

    @Test
    @DisplayName("잘못된 비밀번호 로그인 시 401")
    fun loginWrongPasswordReturns401() {
        val email = uniqueEmail()
        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
        restTemplate.exchange("/api/v1/auth/signup", HttpMethod.POST, HttpEntity("""{"email":"$email","password":"password123"}""", headers), String::class.java)
        val loginRes = restTemplate.exchange("/api/v1/auth/login", HttpMethod.POST, HttpEntity("""{"email":"$email","password":"wrong"}""", headers), String::class.java)
        assertEquals(HttpStatus.UNAUTHORIZED, loginRes.statusCode, "actual: ${loginRes.statusCode}, body: ${loginRes.body}")
    }

    @Test
    @DisplayName("토큰 없이 User Profile API 호출 시 401 또는 403")
    fun userProfileWithoutTokenReturns401Or403() {
        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
        val res = restTemplate.exchange("/api/v1/user/profile", HttpMethod.PUT, HttpEntity(mapOf("mbti" to "INTJ"), headers), String::class.java)
        assertTrue(
            res.statusCode == HttpStatus.UNAUTHORIZED || res.statusCode == HttpStatus.FORBIDDEN,
            "unauthenticated access should return 401 or 403, actual: ${res.statusCode}"
        )
    }
}
