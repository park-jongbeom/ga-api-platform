package com.goalmond.api.service.matching

import com.goalmond.api.domain.entity.School
import com.goalmond.api.repository.SchoolEmbeddingRepository
import com.goalmond.api.repository.SchoolRepository
import com.goalmond.api.service.ai.GeminiClient
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * School 임베딩 서비스 (GAM-3, Phase 2).
 * 
 * School 데이터를 텍스트로 변환하여 Gemini API로 임베딩한 후 pgvector에 저장합니다.
 */
@Service
class EmbeddingService(
    private val geminiClient: GeminiClient,
    private val schoolRepository: SchoolRepository,
    private val schoolEmbeddingRepository: SchoolEmbeddingRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    /**
     * School 엔티티를 임베딩 텍스트로 변환.
     * 
     * 포함 정보:
     * - 학교명, 유형, 위치
     * - 학비, 생활비
     * - 설명
     * - 합격률, 편입률, 졸업률
     */
    fun buildSchoolText(school: School): String {
        return buildString {
            appendLine("학교명: ${school.name}")
            appendLine("유형: ${translateType(school.type)}")
            appendLine("위치: ${school.city}, ${school.state}")
            school.tuition?.let { appendLine("학비: $$it/년") }
            school.livingCost?.let { appendLine("생활비: $$it/년") }
            school.description?.let { appendLine("설명: $it") }
            school.acceptanceRate?.let { appendLine("합격률: $it%") }
            school.transferRate?.let { appendLine("편입률: $it%") }
            school.graduationRate?.let { appendLine("졸업률: $it%") }
            school.ranking?.let { appendLine("랭킹: $it") }
        }.trim()
    }
    
    /**
     * School 하나를 임베딩하여 저장.
     * 
     * @param school School 엔티티
     * @return 성공 여부
     */
    @Transactional
    fun embedSchool(school: School): Boolean {
        return try {
            val text = buildSchoolText(school)
            val embedding = geminiClient.embedContent(text)
            val embeddingStr = embedding.joinToString(prefix = "[", postfix = "]")
            
            schoolEmbeddingRepository.insertOrUpdateEmbedding(
                schoolId = school.id!!,
                embeddingText = text,
                embeddingVector = embeddingStr
            )
            
            logger.info("School embedded successfully: ${school.name} (${school.id})")
            true
        } catch (e: Exception) {
            logger.error("Failed to embed school: ${school.name} (${school.id})", e)
            false
        }
    }
    
    /**
     * 모든 School을 임베딩 (배치 작업).
     * 
     * @return 성공 개수
     */
    @Transactional
    fun embedAllSchools(): Int {
        val schools = schoolRepository.findAll()
        logger.info("Starting to embed ${schools.size} schools")
        
        var successCount = 0
        var failureCount = 0
        
        schools.forEachIndexed { index, school ->
            if (embedSchool(school)) {
                successCount++
            } else {
                failureCount++
            }
            
            // 진행 상황 로깅 (10개마다)
            if ((index + 1) % 10 == 0) {
                logger.info("Progress: ${index + 1}/${schools.size} schools processed")
            }
            
            // Gemini API Rate Limiting 방지 (1초 대기)
            if (index < schools.size - 1) {
                Thread.sleep(1000)
            }
        }
        
        logger.info("Embedding completed: $successCount succeeded, $failureCount failed")
        return successCount
    }
    
    /**
     * School 유형 한글 변환.
     */
    private fun translateType(type: String): String {
        return when (type.lowercase()) {
            "university" -> "4년제 대학"
            "community_college" -> "커뮤니티 칼리지"
            "vocational" -> "직업학교"
            "elementary" -> "초등학교"
            else -> type
        }
    }
}
