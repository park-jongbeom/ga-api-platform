package com.goalmond.user.controller

import com.goalmond.common.dto.ApiResponse
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/users")
class UserController {
    
    @GetMapping("/{userId}")
    fun getUser(@PathVariable userId: String): ApiResponse<Map<String, String>> {
        // TODO: 구현 필요
        return ApiResponse.success(mapOf("userId" to userId))
    }
    
    @GetMapping("/{userId}/profile")
    fun getUserProfile(@PathVariable userId: String): ApiResponse<Map<String, String>> {
        // TODO: 구현 필요
        return ApiResponse.success(mapOf("userId" to userId, "type" to "profile"))
    }
}
