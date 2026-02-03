package com.goalmond.api.controller

import com.goalmond.api.domain.dto.ApiResponse
import com.goalmond.api.domain.dto.MatchingResponse
import com.goalmond.api.service.matching.MatchingEngineService
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

/**
 * 실제 매칭 API Controller (GAM-3, Phase 9).
 * 
 * local/lightsail 프로파일에서만 활성화.
 * default 프로파일에서는 MockMatchingController 사용.
 */
@RestController
@RequestMapping("/api/v1/matching")
@Profile("local", "lightsail")
class MatchingController(
    private val matchingEngineService: MatchingEngineService
) {
    
    /**
     * 매칭 실행 API.
     * 
     * JWT 인증 필수.
     */
    @PostMapping("/run")
    @PreAuthorize("isAuthenticated()")
    fun runMatching(
        @AuthenticationPrincipal principal: String?,
        @RequestBody request: MatchingRunRequest
    ): ResponseEntity<ApiResponse<MatchingResponse>> {
        val userId = UUID.fromString(request.userId)
        val result = matchingEngineService.executeMatching(userId)
        
        return ResponseEntity.ok(ApiResponse(success = true, data = result))
    }
    
    /**
     * 최신 매칭 결과 조회 (추후 구현).
     * 현재는 실시간 매칭만 지원.
     */
    @GetMapping("/result")
    @PreAuthorize("isAuthenticated()")
    fun getLatestMatchingResult(
        @AuthenticationPrincipal principal: String?
    ): ResponseEntity<ApiResponse<String>> {
        return ResponseEntity.ok(
            ApiResponse(
                success = false,
                code = "NOT_IMPLEMENTED",
                message = "매칭 이력 조회는 Week 4에 구현됩니다. /api/v1/matching/run을 사용하세요."
            )
        )
    }
    
    data class MatchingRunRequest(
        val userId: String
    )
}
