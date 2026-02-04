package com.goalmond.api.service.research

import com.goalmond.api.domain.research.PromptTemplate
import com.goalmond.api.domain.research.ResearchStage
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

/**
 * 범용 프롬프트 저장소.
 * 카테고리별 프롬프트 등록 및 조회.
 */
@Component
class PromptRepository {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val prompts = ConcurrentHashMap<String, PromptTemplate>()

    @PostConstruct
    fun initialize() {
        VocationalPrompts.register(this)
        logger.info("프롬프트 로드 완료: ${prompts.size}개")
    }

    fun add(prompt: PromptTemplate) {
        prompts[prompt.id] = prompt
    }

    fun findById(id: String): PromptTemplate? = prompts[id]

    fun findByCategory(category: String): List<PromptTemplate> =
        prompts.values.filter { it.category == category }.sortedBy { it.stage.order }

    fun findByCategoryAndStage(category: String, stage: ResearchStage): List<PromptTemplate> =
        prompts.values.filter { it.category == category && it.stage == stage }.sortedBy { it.id }

    fun findAll(): List<PromptTemplate> = prompts.values.toList()
}
