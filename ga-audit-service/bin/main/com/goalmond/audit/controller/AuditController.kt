package com.goalmond.audit.controller

import com.goalmond.common.dto.ApiResponse
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
 * 감사 로그 API 컨트롤러
 * 
 * 데이터베이스 변경 이력 및 감사 로그 조회 기능을 제공합니다.
 * 
 * @author Go Almond
 * @version 1.0.0
 */
@Tag(
    name = "감사 로그 API",
    description = """
        데이터베이스 변경 이력 및 감사 로그 조회를 위한 REST API입니다.
        
        ## 주요 기능
        - 감사 로그 조회 (필터링 및 페이징 지원)
        - 테이블별 로그 조회
        - 레코드별 로그 조회
        
        ## 인증 필요
        - 모든 API는 JWT Access Token이 필요합니다.
        - 요청 헤더에 `Authorization: Bearer {accessToken}` 포함 필요
        
        ## 사용 방법
        1. 인증 서비스에서 Access Token을 발급받습니다.
        2. 조회 조건에 따라 쿼리 파라미터를 설정합니다.
        3. 페이징을 사용하여 대량의 로그를 효율적으로 조회합니다.
        
        ## 필터링 옵션
        - `tableName`: 특정 테이블의 로그만 조회
        - `recordId`: 특정 레코드의 로그만 조회
        - `page`: 페이지 번호 (0부터 시작)
        - `size`: 페이지당 항목 수 (기본값: 20)
    """
)
@RestController
@RequestMapping("/api/audit")
class AuditController {
    
    @Operation(
        summary = "감사 로그 조회",
        description = """
            데이터베이스 변경 이력 및 감사 로그를 조회합니다.
            필터링 및 페이징 기능을 지원합니다.
            
            ## 요청 형식
            - Method: GET
            - Path: `/api/audit/logs`
            - Authorization: `Bearer {accessToken}` (필수)
            
            ## 쿼리 파라미터
            - `tableName` (선택): 조회할 테이블 이름 (예: "users", "academic_profiles")
            - `recordId` (선택): 조회할 레코드 ID (UUID 형식)
            - `page` (선택): 페이지 번호 (기본값: 0, 0부터 시작)
            - `size` (선택): 페이지당 항목 수 (기본값: 20, 최대: 100)
            
            ## 응답 형식
            - 성공 시: 감사 로그 목록 및 페이징 정보
            
            ## 페이징 정보
            - `totalElements`: 전체 항목 수
            - `totalPages`: 전체 페이지 수
            - `currentPage`: 현재 페이지 번호
            - `pageSize`: 페이지당 항목 수
            
            ## 예시 요청
            ```
            GET /api/audit/logs?tableName=users&page=0&size=20
            GET /api/audit/logs?tableName=users&recordId=123e4567-e89b-12d3-a456-426614174000
            ```
            
            ## 예시 응답
            ```json
            {
              "success": true,
              "data": {
                "logs": [
                  {
                    "id": "audit-1",
                    "tableName": "users",
                    "recordId": "123e4567-e89b-12d3-a456-426614174000",
                    "action": "INSERT",
                    "oldValues": null,
                    "newValues": {
                      "email": "user@example.com",
                      "name": "홍길동"
                    },
                    "userId": "admin-1",
                    "timestamp": "2024-01-01T00:00:00"
                  }
                ],
                "pagination": {
                  "totalElements": 100,
                  "totalPages": 5,
                  "currentPage": 0,
                  "pageSize": 20
                }
              },
              "message": null,
              "timestamp": "2024-01-01T00:00:00"
            }
            ```
        """,
        tags = ["감사 로그 API"]
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "감사 로그 조회 성공",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ApiResponse::class),
                    examples = [ExampleObject(
                        value = """
                            {
                              "success": true,
                              "data": {
                                "logs": [
                                  {
                                    "id": "audit-1",
                                    "tableName": "users",
                                    "recordId": "123e4567-e89b-12d3-a456-426614174000",
                                    "action": "INSERT",
                                    "oldValues": null,
                                    "newValues": {
                                      "email": "user@example.com",
                                      "name": "홍길동"
                                    },
                                    "userId": "admin-1",
                                    "timestamp": "2024-01-01T00:00:00"
                                  }
                                ],
                                "pagination": {
                                  "totalElements": 100,
                                  "totalPages": 5,
                                  "currentPage": 0,
                                  "pageSize": 20
                                }
                              },
                              "message": null,
                              "timestamp": "2024-01-01T00:00:00"
                            }
                        """
                    )]
                )]
            ),
            SwaggerApiResponse(
                responseCode = "400",
                description = "잘못된 요청 (파라미터 형식 오류)"
            ),
            SwaggerApiResponse(
                responseCode = "401",
                description = "인증되지 않은 요청"
            )
        ]
    )
    @GetMapping("/logs")
    fun getLogs(
        @Parameter(
            description = "조회할 테이블 이름 (선택사항)",
            example = "users"
        )
        @RequestParam(required = false) tableName: String?,
        
        @Parameter(
            description = "조회할 레코드 ID (UUID 형식, 선택사항)",
            example = "123e4567-e89b-12d3-a456-426614174000"
        )
        @RequestParam(required = false) recordId: String?,
        
        @Parameter(
            description = "페이지 번호 (0부터 시작, 기본값: 0)",
            example = "0"
        )
        @RequestParam(defaultValue = "0") page: Int,
        
        @Parameter(
            description = "페이지당 항목 수 (기본값: 20, 최대: 100)",
            example = "20"
        )
        @RequestParam(defaultValue = "20") size: Int
    ): ApiResponse<Map<String, Any>> {
        // TODO: 구현 필요
        return ApiResponse.success(mapOf("message" to "Audit logs endpoint"))
    }
}
