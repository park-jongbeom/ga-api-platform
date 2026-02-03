package com.goalmond.api.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * 매칭 점수 가중치 설정 (GAM-78).
 * 학업·영어·예산·지역·기간·진로 등 적합도 점수 가중치.
 */
@Configuration
class WeightConfigConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "app.matching.weights")
    fun weightConfig(): WeightConfig = WeightConfig()
}

data class WeightConfig(
    var academic: Double = 0.25,
    var english: Double = 0.20,
    var budget: Double = 0.20,
    var location: Double = 0.15,
    var duration: Double = 0.10,
    var career: Double = 0.10
)
