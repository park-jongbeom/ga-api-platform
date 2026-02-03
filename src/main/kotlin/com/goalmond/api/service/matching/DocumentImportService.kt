package com.goalmond.api.service.matching

import com.goalmond.api.controller.admin.ImportResult
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.BufferedReader
import java.util.UUID

/**
 * 문서 임포트 서비스.
 * 
 * CSV/JSON 파일을 파싱하여 문서를 임베딩하고 VectorStore에 저장합니다.
 */
@Service
class DocumentImportService(
    private val embeddingService: EmbeddingService,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    /**
     * 학교 문서 임포트.
     * 
     * CSV 형식:
     * school_id,document_type,title,content,metadata
     * 
     * @param csvFile CSV 파일
     * @return 임포트 결과
     */
    fun importSchoolDocuments(csvFile: MultipartFile): ImportResult {
        logger.info("Starting school documents import: ${csvFile.originalFilename}")
        
        val records = parseCsv(csvFile)
        var successCount = 0
        var failureCount = 0
        
        records.forEachIndexed { index, record ->
            try {
                val schoolId = UUID.fromString(record["school_id"])
                val docType = record["document_type"] ?: throw IllegalArgumentException("document_type is required")
                val title = record["title"] ?: throw IllegalArgumentException("title is required")
                val content = record["content"] ?: throw IllegalArgumentException("content is required")
                val metadata = parseMetadata(record["metadata"])
                
                // EmbeddingService로 임베딩 + 저장
                if (embeddingService.embedSchoolDocument(schoolId, docType, title, content, metadata)) {
                    successCount++
                } else {
                    failureCount++
                }
                
                // 진행 상황 로깅
                if ((index + 1) % 10 == 0) {
                    logger.info("Progress: ${index + 1}/${records.size} documents processed")
                }
                
                // Rate limiting (60 req/min)
                if (index < records.size - 1) {
                    Thread.sleep(1000)
                }
            } catch (e: Exception) {
                logger.error("Failed to import document: ${record["title"]}", e)
                failureCount++
            }
        }
        
        logger.info("School documents import completed: success=$successCount, failed=$failureCount")
        return ImportResult(successCount, failureCount)
    }
    
    /**
     * 프로그램 문서 임포트.
     * 
     * CSV 형식:
     * program_id,document_type,title,content,metadata
     * 
     * @param csvFile CSV 파일
     * @return 임포트 결과
     */
    fun importProgramDocuments(csvFile: MultipartFile): ImportResult {
        logger.info("Starting program documents import: ${csvFile.originalFilename}")
        
        val records = parseCsv(csvFile)
        var successCount = 0
        var failureCount = 0
        
        records.forEachIndexed { index, record ->
            try {
                val programId = UUID.fromString(record["program_id"])
                val docType = record["document_type"] ?: throw IllegalArgumentException("document_type is required")
                val title = record["title"] ?: throw IllegalArgumentException("title is required")
                val content = record["content"] ?: throw IllegalArgumentException("content is required")
                val metadata = parseMetadata(record["metadata"])
                
                // EmbeddingService로 임베딩 + 저장
                if (embeddingService.embedProgramDocument(programId, docType, title, content, metadata)) {
                    successCount++
                } else {
                    failureCount++
                }
                
                // 진행 상황 로깅
                if ((index + 1) % 10 == 0) {
                    logger.info("Progress: ${index + 1}/${records.size} documents processed")
                }
                
                // Rate limiting
                if (index < records.size - 1) {
                    Thread.sleep(1000)
                }
            } catch (e: Exception) {
                logger.error("Failed to import document: ${record["title"]}", e)
                failureCount++
            }
        }
        
        logger.info("Program documents import completed: success=$successCount, failed=$failureCount")
        return ImportResult(successCount, failureCount)
    }
    
    /**
     * CSV 파일 파싱.
     * 
     * 첫 번째 행을 헤더로 사용하고, 나머지 행을 Map으로 변환합니다.
     */
    private fun parseCsv(file: MultipartFile): List<Map<String, String>> {
        val reader = BufferedReader(file.inputStream.reader())
        
        // 헤더 읽기
        val header = reader.readLine()?.split(",") 
            ?: throw IllegalArgumentException("CSV file is empty")
        
        // 데이터 행 파싱
        return reader.lineSequence()
            .filter { it.isNotBlank() }
            .map { line ->
                // CSV 파싱 (따옴표 처리 포함)
                val values = parseCsvLine(line)
                header.zip(values).toMap()
            }
            .toList()
    }
    
    /**
     * CSV 라인 파싱 (따옴표 처리).
     * 
     * "value with, comma" 형식을 올바르게 처리합니다.
     */
    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        
        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    result.add(current.toString().trim())
                    current = StringBuilder()
                }
                else -> current.append(char)
            }
        }
        
        result.add(current.toString().trim())
        return result
    }
    
    /**
     * 메타데이터 JSON 파싱.
     * 
     * 예: {"rating": 4.5, "author": "익명"}
     */
    private fun parseMetadata(metadataJson: String?): Map<String, Any> {
        if (metadataJson.isNullOrBlank()) {
            return emptyMap()
        }
        
        return try {
            objectMapper.readValue<Map<String, Any>>(metadataJson)
        } catch (e: Exception) {
            logger.warn("Failed to parse metadata JSON: $metadataJson", e)
            emptyMap()
        }
    }
}
