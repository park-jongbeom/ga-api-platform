package com.goalmond.api.controller.admin

import com.goalmond.api.service.matching.DocumentImportService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

/**
 * 문서 임포트 API 컨트롤러 (Admin용).
 * 
 * CSV/JSON 파일을 업로드하여 RAG 문서를 임베딩하고 저장합니다.
 */
@RestController
@RequestMapping("/api/v1/admin/documents")
class DocumentImportController(
    private val documentImportService: DocumentImportService
) {
    
    /**
     * 학교 문서 임포트.
     * 
     * CSV 형식:
     * school_id,document_type,title,content,metadata
     * 
     * @param file CSV 파일
     * @return 임포트 결과
     */
    @PostMapping("/schools/import")
    fun importSchoolDocuments(
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<ImportResult> {
        val result = documentImportService.importSchoolDocuments(file)
        return ResponseEntity.ok(result)
    }
    
    /**
     * 프로그램 문서 임포트.
     * 
     * CSV 형식:
     * program_id,document_type,title,content,metadata
     * 
     * @param file CSV 파일
     * @return 임포트 결과
     */
    @PostMapping("/programs/import")
    fun importProgramDocuments(
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<ImportResult> {
        val result = documentImportService.importProgramDocuments(file)
        return ResponseEntity.ok(result)
    }
    
    /**
     * 임포트 상태 조회 (향후 확장).
     */
    @GetMapping("/import-status")
    fun getImportStatus(): ResponseEntity<String> {
        return ResponseEntity.ok("Import API is available")
    }
}

/**
 * 임포트 결과 DTO.
 */
data class ImportResult(
    val successCount: Int,
    val failureCount: Int,
    val totalCount: Int = successCount + failureCount,
    val message: String = "임포트 완료: 성공 $successCount, 실패 $failureCount"
)
