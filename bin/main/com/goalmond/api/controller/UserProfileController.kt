package com.goalmond.api.controller

import com.goalmond.api.domain.dto.ApiResponse
import com.goalmond.api.domain.dto.CompleteUserProfileResponse
import com.goalmond.api.domain.dto.EducationRequest
import com.goalmond.api.domain.dto.PreferenceRequest
import com.goalmond.api.domain.dto.ProfileUpdateRequest
import com.goalmond.api.service.UserProfileService
import jakarta.validation.Valid
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/user")
@Profile("local", "lightsail")
class UserProfileController(private val userProfileService: UserProfileService) {

    private fun currentUserId(): UUID {
        val principal = SecurityContextHolder.getContext().authentication?.principal as? String
            ?: throw IllegalStateException("인증되지 않았습니다")
        return UUID.fromString(principal)
    }

    @GetMapping("/profile")
    fun getUserProfile(): ResponseEntity<ApiResponse<CompleteUserProfileResponse>> {
        val data = userProfileService.getUserProfile(currentUserId())
        return ResponseEntity.ok(ApiResponse(success = true, data = data))
    }

    @PutMapping("/profile")
    fun updateProfile(@Valid @RequestBody request: ProfileUpdateRequest): ResponseEntity<ApiResponse<Unit>> {
        userProfileService.updateProfile(currentUserId(), request)
        return ResponseEntity.ok(ApiResponse(success = true, data = null))
    }

    @PostMapping("/education")
    fun saveEducation(@Valid @RequestBody request: EducationRequest): ResponseEntity<ApiResponse<Unit>> {
        userProfileService.saveEducation(currentUserId(), request)
        return ResponseEntity.ok(ApiResponse(success = true, data = null))
    }

    @PostMapping("/preference")
    fun savePreference(@Valid @RequestBody request: PreferenceRequest): ResponseEntity<ApiResponse<Unit>> {
        userProfileService.savePreference(currentUserId(), request)
        return ResponseEntity.ok(ApiResponse(success = true, data = null))
    }
}
