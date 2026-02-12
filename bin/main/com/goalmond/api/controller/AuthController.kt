package com.goalmond.api.controller

import com.goalmond.api.domain.dto.ApiResponse
import com.goalmond.api.domain.dto.LoginRequest
import com.goalmond.api.domain.dto.SignupRequest
import com.goalmond.api.service.AuthException
import com.goalmond.api.service.AuthService
import jakarta.validation.Valid
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/api/v1/auth")
@Profile("local", "lightsail")
class AuthController(private val authService: AuthService) {

    @PostMapping("/signup")
    fun signup(@Valid @RequestBody request: SignupRequest): ResponseEntity<ApiResponse<*>> {
        return try {
            val response = authService.signup(request)
            ResponseEntity.ok(ApiResponse(success = true, data = response))
        } catch (e: AuthException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse(success = false, data = null, code = e.code, message = e.message, timestamp = Instant.now())
            )
        }
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<ApiResponse<*>> {
        return try {
            val response = authService.login(request)
            ResponseEntity.ok(ApiResponse(success = true, data = response))
        } catch (e: AuthException) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ApiResponse(success = false, data = null, code = e.code, message = e.message, timestamp = Instant.now())
            )
        }
    }
}
