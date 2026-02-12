package com.goalmond.api.controller

import com.goalmond.api.domain.dto.ApiResponse
import com.goalmond.api.domain.research.PromptTemplate
import com.goalmond.api.domain.research.ResearchReport
import com.goalmond.api.domain.research.ResearchRequest
import com.goalmond.api.domain.research.ResearchResult
import com.goalmond.api.domain.research.ResearchStage
import com.goalmond.api.service.research.PromptRepository
import com.goalmond.api.service.research.ResearchAgentService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 범용 조사 에이전트 API.
 * 매칭 결과 보완용 심층 정보 제공.
 */
@RestController
@RequestMapping("/api/v1/research")
class ResearchController(
    private val researchAgentService: ResearchAgentService,
    private val promptRepository: PromptRepository
) {

    /**
     * 전체 4단계 조사 실행.
     * POST /api/v1/research/full
     */
    @PostMapping("/full")
    fun runFullResearch(@RequestBody request: ResearchRequest): ResponseEntity<ApiResponse<ResearchReport>> {
        val report = researchAgentService.runFullResearch(request)
        return ResponseEntity.ok(ApiResponse(success = true, data = report))
    }

    /**
     * 특정 단계 조사 실행.
     * POST /api/v1/research/stage/{stage}
     */
    @PostMapping("/stage/{stage}")
    fun runStageResearch(
        @PathVariable stage: ResearchStage,
        @RequestBody request: ResearchRequest
    ): ResponseEntity<ApiResponse<List<ResearchResult>>> {
        val variables = mapOf(
            "field" to (request.field ?: "일반"),
            "state" to (request.state ?: "California"),
            "cities" to (request.cities ?: "LA, SF")
        )
        val results = researchAgentService.researchStage(request.category, stage, variables)
        return ResponseEntity.ok(ApiResponse(success = true, data = results))
    }

    /**
     * 단일 프롬프트 실행.
     * POST /api/v1/research/prompt/{promptId}
     */
    @PostMapping("/prompt/{promptId}")
    fun runPrompt(
        @PathVariable promptId: String,
        @RequestBody variables: Map<String, Any>
    ): ResponseEntity<ApiResponse<ResearchResult>> {
        val result = researchAgentService.researchPrompt(promptId, variables)
        return ResponseEntity.ok(ApiResponse(success = true, data = result))
    }

    /**
     * 프롬프트 목록 조회.
     * GET /api/v1/research/prompts?category=vocational
     */
    @GetMapping("/prompts")
    fun getPrompts(
        @RequestParam(required = false) category: String?
    ): ResponseEntity<ApiResponse<List<PromptTemplate>>> {
        val prompts = if (category != null) {
            promptRepository.findByCategory(category)
        } else {
            promptRepository.findAll()
        }
        return ResponseEntity.ok(ApiResponse(success = true, data = prompts))
    }
}
