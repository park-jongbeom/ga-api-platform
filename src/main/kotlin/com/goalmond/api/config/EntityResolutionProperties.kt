package com.goalmond.api.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.entity-resolution")
data class EntityResolutionProperties(
    val aliasesPath: String = "entity-resolution/aliases.json",
    val similarityThreshold: Double = 0.85,
    val cacheSize: Int = 512,
    val cacheTtlMinutes: Long = 10,
    val defaultConfidenceScore: Double = 0.75
) {
    init {
        require(cacheSize > 0) { "cacheSize must be positive" }
        require(cacheTtlMinutes > 0) { "cacheTtlMinutes must be positive" }
        require(similarityThreshold in 0.0..1.0) { "similarityThreshold must be between 0 and 1" }
    }
}
