package com.goalmond.auth.controller

import com.goalmond.common.dto.ApiResponse
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController {
    
    @PostMapping("/login")
    fun login(): ApiResponse<Map<String, String>> {
        // TODO: 구현 필요
        return ApiResponse.success(mapOf("message" to "Login endpoint"))
    }
    
    @PostMapping("/refresh")
    fun refresh(): ApiResponse<Map<String, String>> {
        // TODO: 구현 필요
        return ApiResponse.success(mapOf("message" to "Refresh endpoint"))
    }
    
    @PostMapping("/logout")
    fun logout(): ApiResponse<Unit> {
        // TODO: 구현 필요
        return ApiResponse.success(Unit, "Logout successful")
    }
}
