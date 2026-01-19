package com.goalmond.audit.controller

import com.goalmond.common.dto.ApiResponse
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/audit")
class AuditController {
    
    @GetMapping("/logs")
    fun getLogs(
        @RequestParam(required = false) tableName: String?,
        @RequestParam(required = false) recordId: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ApiResponse<Map<String, Any>> {
        // TODO: 구현 필요
        return ApiResponse.success(mapOf("message" to "Audit logs endpoint"))
    }
}
