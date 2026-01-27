package com.goalmond.auth.controller

import com.goalmond.auth.domain.dto.LoginRequest
import com.goalmond.auth.domain.dto.RefreshTokenRequest
import com.goalmond.auth.domain.dto.TokenResponse
import com.goalmond.common.dto.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*

/**
 * 인증/인가 API 컨트롤러
 * 
 * 사용자 로그인, 토큰 갱신, 로그아웃 기능을 제공합니다.
 * 
 * @author Go Almond
 * @version 1.0.0
 */
@Tag(
    name = "인증/인가 API",
    description = """
        사용자 인증 및 인가를 위한 REST API입니다.
        
        ## 주요 기능
        - 사용자 로그인 (JWT 토큰 발급)
        - Access Token 갱신 (Refresh Token 사용)
        - 사용자 로그아웃 (토큰 무효화)
        
        ## 인증 방식
        - JWT (JSON Web Token) 기반 인증
        - Access Token: 1시간 유효
        - Refresh Token: 7일 유효
        
        ## 사용 방법
        1. `/api/auth/login` 엔드포인트로 로그인하여 Access Token과 Refresh Token을 받습니다.
        2. 이후 API 호출 시 `Authorization: Bearer {accessToken}` 헤더를 포함합니다.
        3. Access Token이 만료되면 `/api/auth/refresh` 엔드포인트로 토큰을 갱신합니다.
        4. 로그아웃 시 `/api/auth/logout` 엔드포인트를 호출하여 토큰을 무효화합니다.
    """
)
@RestController
@RequestMapping("/api/auth")
class AuthController {
    
    @Operation(
        summary = "사용자 로그인",
        description = """
            이메일과 비밀번호를 사용하여 사용자를 인증하고 JWT 토큰을 발급합니다.
            
            ## 요청 형식
            - Content-Type: `application/json`
            - Body: `{ "email": "user@example.com", "password": "password123" }`
            
            ## 응답 형식
            - 성공 시: Access Token과 Refresh Token을 포함한 응답
            - 실패 시: 401 Unauthorized 또는 400 Bad Request
            
            ## 토큰 정보
            - Access Token: API 호출 시 사용 (1시간 유효)
            - Refresh Token: Access Token 갱신 시 사용 (7일 유효)
            
            ## 예시
            ```json
            {
              "success": true,
              "data": {
                "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                "tokenType": "Bearer",
                "expiresIn": 3600
              },
              "message": "로그인 성공",
              "timestamp": "2024-01-01T00:00:00"
            }
            ```
        """,
        tags = ["인증/인가 API"]
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "로그인 성공",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ApiResponse::class),
                    examples = [ExampleObject(
                        value = """
                            {
                              "success": true,
                              "data": {
                                "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                "tokenType": "Bearer",
                                "expiresIn": 3600
                              },
                              "message": "로그인 성공",
                              "timestamp": "2024-01-01T00:00:00"
                            }
                        """
                    )]
                )]
            ),
            SwaggerApiResponse(
                responseCode = "400",
                description = "잘못된 요청 (이메일 또는 비밀번호 형식 오류)"
            ),
            SwaggerApiResponse(
                responseCode = "401",
                description = "인증 실패 (이메일 또는 비밀번호 불일치)"
            )
        ]
    )
    @PostMapping("/login")
    fun login(
        @RequestBody request: LoginRequest
    ): ApiResponse<TokenResponse> {
        // TODO: 구현 필요
        return ApiResponse.success(
            TokenResponse(
                accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                refreshToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                tokenType = "Bearer",
                expiresIn = 3600
            )
        )
    }
    
    @Operation(
        summary = "Access Token 갱신",
        description = """
            Refresh Token을 사용하여 새로운 Access Token을 발급받습니다.
            
            ## 요청 형식
            - Content-Type: `application/json`
            - Authorization: `Bearer {refreshToken}` (선택사항, Body에 포함 가능)
            - Body: `{ "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." }`
            
            ## 응답 형식
            - 성공 시: 새로운 Access Token과 Refresh Token
            - 실패 시: 401 Unauthorized (Refresh Token 만료 또는 무효)
            
            ## 사용 시나리오
            1. Access Token이 만료된 경우
            2. 클라이언트가 새로운 Access Token이 필요한 경우
            
            ## 예시
            ```json
            {
              "success": true,
              "data": {
                "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                "tokenType": "Bearer",
                "expiresIn": 3600
              },
              "message": "토큰 갱신 성공",
              "timestamp": "2024-01-01T00:00:00"
            }
            ```
        """,
        tags = ["인증/인가 API"]
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "토큰 갱신 성공"
            ),
            SwaggerApiResponse(
                responseCode = "401",
                description = "Refresh Token 만료 또는 무효"
            )
        ]
    )
    @PostMapping("/refresh")
    fun refresh(
        @RequestBody request: RefreshTokenRequest
    ): ApiResponse<TokenResponse> {
        // TODO: 구현 필요
        return ApiResponse.success(
            TokenResponse(
                accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                refreshToken = request.refreshToken,
                tokenType = "Bearer",
                expiresIn = 3600
            )
        )
    }
    
    @Operation(
        summary = "사용자 로그아웃",
        description = """
            현재 사용자를 로그아웃하고 토큰을 무효화합니다.
            
            ## 요청 형식
            - Authorization: `Bearer {accessToken}` (필수)
            - Content-Type: `application/json`
            
            ## 응답 형식
            - 성공 시: 로그아웃 성공 메시지
            - 실패 시: 401 Unauthorized (토큰 없음 또는 만료)
            
            ## 동작 방식
            1. Access Token을 Redis에서 제거하여 무효화
            2. Refresh Token도 함께 무효화 (선택사항)
            3. 이후 해당 토큰으로는 API 호출 불가
            
            ## 예시
            ```json
            {
              "success": true,
              "data": null,
              "message": "로그아웃 성공",
              "timestamp": "2024-01-01T00:00:00"
            }
            ```
        """,
        tags = ["인증/인가 API"]
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "로그아웃 성공"
            ),
            SwaggerApiResponse(
                responseCode = "401",
                description = "인증되지 않은 요청"
            )
        ]
    )
    @PostMapping("/logout")
    fun logout(): ApiResponse<Unit> {
        // TODO: 구현 필요
        return ApiResponse.success(Unit, "Logout successful")
    }
}
