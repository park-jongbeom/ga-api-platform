package com.goalmond.api.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import kotlin.math.abs

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
    var academic: Double = 0.20,    // 학업 적합도 20%
    var english: Double = 0.15,     // 영어 적합도 15%
    var budget: Double = 0.15,      // 예산 적합도 15%
    var location: Double = 0.10,    // 지역 선호 10%
    var duration: Double = 0.10,    // 기간 적합도 10%
    var career: Double = 0.30       // 진로 연계성 30%
) {
    init {
        // 가중치 합 검증 (100% = 1.0)
        val sum = academic + english + budget + location + duration + career
        require(abs(sum - 1.0) < 0.001) {
            "Weight sum must be 1.0, but was $sum"
        }
    }
}
