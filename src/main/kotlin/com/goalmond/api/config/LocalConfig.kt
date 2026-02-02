package com.goalmond.api.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("local")
@EnableConfigurationProperties(TestUserSeedProperties::class)
class LocalConfig
