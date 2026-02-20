package com.goalmond.api.config

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource

@Configuration
@EnableConfigurationProperties(EntityResolutionProperties::class)
class EntityResolutionConfiguration(
    private val objectMapper: ObjectMapper,
    private val properties: EntityResolutionProperties,
) {

    @Bean("entityAliasDictionary")
    fun entityAliasDictionary(): Map<String, List<String>> {
        val resource = ClassPathResource(properties.aliasesPath)
        if (!resource.exists()) {
            throw IllegalStateException("Entity alias dictionary not found: ${properties.aliasesPath}")
        }
        return objectMapper.readValue(
            resource.inputStream,
            object : TypeReference<Map<String, List<String>>>() {}
        )
    }
}
