package com.goalmond.user.controller

import com.goalmond.common.dto.ApiResponse
import com.goalmond.user.domain.dto.UserProfileResponseDto
import com.goalmond.user.domain.dto.UserResponseDto
import java.time.LocalDateTime
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*

/**
 * 사용자 프로필 API 컨트롤러
 * 
 * 사용자 정보 및 프로필 조회 기능을 제공합니다.
 * 
 * @author Go Almond
 * @version 1.0.0
 */
@Tag(
    name = "사용자 프로필 API",
    description = """
        사용자 정보 및 프로필 조회를 위한 REST API입니다.
        
        ## 주요 기능
        - 사용자 기본 정보 조회
        - 사용자 상세 프로필 조회 (학업, 재정, 선호도 포함)
        
        ## 인증 필요
        - 모든 API는 JWT Access Token이 필요합니다.
        - 요청 헤더에 `Authorization: Bearer {accessToken}` 포함 필요
        
        ## 사용 방법
        1. 인증 서비스에서 Access Token을 발급받습니다.
        2. 사용자 정보 조회 시 `Authorization` 헤더에 토큰을 포함합니다.
        3. 사용자 ID는 UUID 형식입니다.
    """
)
@RestController
@RequestMapping("/api/users")
class UserController {
    
    @Operation(
        summary = "사용자 기본 정보 조회",
        description = """
            사용자 ID를 기반으로 사용자의 기본 정보를 조회합니다.
            
            ## 요청 형식
            - Method: GET
            - Path: `/api/users/{userId}`
            - Authorization: `Bearer {accessToken}` (필수)
            
            ## 경로 파라미터
            - `userId`: 조회할 사용자의 UUID (필수)
            
            ## 응답 형식
            - 성공 시: 사용자 기본 정보 (이메일, 이름 등)
            - 실패 시: 404 Not Found (사용자 없음) 또는 401 Unauthorized
            
            ## 예시
            ```json
            {
              "success": true,
              "data": {
                "userId": "123e4567-e89b-12d3-a456-426614174000",
                "email": "user@example.com",
                "fullName": "홍길동",
                "createdAt": "2024-01-01T00:00:00"
              },
              "message": null,
              "timestamp": "2024-01-01T00:00:00"
            }
            ```
        """,
        tags = ["사용자 프로필 API"]
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "사용자 정보 조회 성공",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ApiResponse::class),
                    examples = [ExampleObject(
                        value = """
                            {
                              "success": true,
                              "data": {
                                "userId": "123e4567-e89b-12d3-a456-426614174000",
                                "email": "user@example.com",
                                "fullName": "홍길동",
                                "createdAt": "2024-01-01T00:00:00"
                              },
                              "message": null,
                              "timestamp": "2024-01-01T00:00:00"
                            }
                        """
                    )]
                )]
            ),
            SwaggerApiResponse(
                responseCode = "401",
                description = "인증되지 않은 요청 (토큰 없음 또는 만료)"
            ),
            SwaggerApiResponse(
                responseCode = "404",
                description = "사용자를 찾을 수 없음"
            )
        ]
    )
    @GetMapping("/{userId}")
    fun getUser(
        @Parameter(
            description = "사용자 UUID",
            required = true,
            example = "123e4567-e89b-12d3-a456-426614174000"
        )
        @PathVariable userId: String
    ): ApiResponse<UserResponseDto> {
        // TODO: 구현 필요
        return ApiResponse.success(
            UserResponseDto(
                userId = userId,
                email = "user@example.com",
                fullName = "홍길동",
                createdAt = LocalDateTime.now()
            )
        )
    }
    
    @Operation(
        summary = "사용자 상세 프로필 조회",
        description = """
            사용자 ID를 기반으로 사용자의 상세 프로필을 조회합니다.
            학업 프로필, 재정 프로필, 사용자 선호도 정보를 포함합니다.
            
            ## 요청 형식
            - Method: GET
            - Path: `/api/users/{userId}/profile`
            - Authorization: `Bearer {accessToken}` (필수)
            
            ## 경로 파라미터
            - `userId`: 조회할 사용자의 UUID (필수)
            
            ## 응답 형식
            - 성공 시: 사용자 상세 프로필 정보
              - 학업 프로필 (Academic Profile)
              - 재정 프로필 (Financial Profile)
              - 사용자 선호도 (User Preference)
            
            ## 예시
            ```json
            {
              "success": true,
              "data": {
                "userId": "123e4567-e89b-12d3-a456-426614174000",
                "email": "user@example.com",
                "fullName": "홍길동",
                "academicProfiles": [
                  {
                    "id": "academic-1",
                    "schoolName": "Seoul University",
                    "degreeType": "Bachelor",
                    "degree": "BACHELOR",
                    "major": "Computer Science",
                    "gpa": 3.8,
                    "gpaScale": 4.0,
                    "graduationDate": "2024-06-15",
                    "institution": "Seoul University"
                  }
                ],
                "financialProfiles": [
                  {
                    "id": "financial-1",
                    "budgetRange": "10000-20000",
                    "totalBudgetUsd": 50000,
                    "tuitionLimitUsd": 30000,
                    "fundingSource": "Personal"
                  }
                ],
                "preferences": [
                  {
                    "id": "pref-1",
                    "targetMajor": "Engineering",
                    "targetLocation": "Seoul",
                    "careerGoal": "Software Developer",
                    "preferredTrack": "Software Developer"
                  }
                ]
              },
              "message": null,
              "timestamp": "2024-01-01T00:00:00"
            }
            ```
        """,
        tags = ["사용자 프로필 API"]
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "사용자 프로필 조회 성공"
            ),
            SwaggerApiResponse(
                responseCode = "401",
                description = "인증되지 않은 요청"
            ),
            SwaggerApiResponse(
                responseCode = "404",
                description = "사용자를 찾을 수 없음"
            )
        ]
    )
    @GetMapping("/{userId}/profile")
    fun getUserProfile(
        @Parameter(
            description = "사용자 UUID",
            required = true,
            example = "123e4567-e89b-12d3-a456-426614174000"
        )
        @PathVariable userId: String
    ): ApiResponse<UserProfileResponseDto> {
        // TODO: 구현 필요
        return ApiResponse.success(
            UserProfileResponseDto(
                userId = userId,
                email = "user@example.com",
                fullName = "홍길동",
                academicProfiles = listOf(
                    UserProfileResponseDto.AcademicProfileDto(
                        id = "academic-1",
                        schoolName = "Seoul University",
                        degreeType = "Bachelor",
                        degree = "BACHELOR",
                        major = "Computer Science",
                        gpa = java.math.BigDecimal("3.8"),
                        gpaScale = java.math.BigDecimal("4.0"),
                        graduationDate = java.time.LocalDate.of(2024, 6, 15),
                        institution = "Seoul University"
                    )
                ),
                financialProfiles = listOf(
                    UserProfileResponseDto.FinancialProfileDto(
                        id = "financial-1",
                        budgetRange = "10000-20000",
                        totalBudgetUsd = 50000,
                        tuitionLimitUsd = 30000,
                        fundingSource = "Personal"
                    )
                ),
                preferences = listOf(
                    UserProfileResponseDto.PreferenceDto(
                        id = "pref-1",
                        targetMajor = "Engineering",
                        targetLocation = "Seoul",
                        careerGoal = "Software Developer",
                        preferredTrack = "Software Developer"
                    )
                )
            )
        )
    }
}
