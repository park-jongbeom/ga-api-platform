package com.goalmond.api.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Profile

@ConfigurationProperties(prefix = "app.seed-test-user")
@Profile("local")
data class TestUserSeedProperties(
    val enabled: Boolean = true,
    val email: String = "test@example.com",
    val password: String = "test1234Z",
    val fullName: String = "테스트 사용자"
)
