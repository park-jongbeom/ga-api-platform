package com.goalmond.api.service.matching

import com.goalmond.api.domain.entity.School
import com.goalmond.api.repository.SchoolEmbeddingRepository
import com.goalmond.api.repository.SchoolRepository
import com.goalmond.api.service.ai.GeminiClient
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * School 임베딩 서비스 (GAM-3, Phase 2 + RAG 확장).
 * 
 * School 데이터를 텍스트로 변환하여 Gemini API로 임베딩한 후 pgvector에 저장합니다.
 * Spring AI VectorStore를 활용하여 문서 임베딩 기능 추가.
 */
@Service
class EmbeddingService(
    private val geminiClient: GeminiClient,
    private val schoolRepository: SchoolRepository,
    private val schoolEmbeddingRepository: SchoolEmbeddingRepository,
    @Qualifier("schoolDocumentVectorStore")
    private val schoolDocumentVectorStore: VectorStore,
    @Qualifier("programDocumentVectorStore")
    private val programDocumentVectorStore: VectorStore
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    /**
     * School 엔티티를 임베딩 텍스트로 변환.
     * 
     * 포함 정보:
     * - 학교명, 유형, 위치
     * - 학비, 생활비
     * - 설명
     * - 합격률, 편입률, 졸업률, 초봉/취업률(가능한 경우)
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
            school.averageSalary?.let { appendLine("초봉(중간값): $$it") }
            school.employmentRate?.let { appendLine("취업률: ${it.stripTrailingZeros().toPlainString()}%") }
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
    
    // ====== RAG 문서 임베딩 메서드 (Spring AI VectorStore 활용) ======
    
    /**
     * 학교 문서를 임베딩하여 VectorStore에 저장.
     * 
     * Spring AI VectorStore가 자동으로 임베딩 생성 및 저장을 처리합니다.
     * 
     * @param schoolId 학교 ID
     * @param docType 문서 타입 (review, admission_guide, statistics, pros_cons)
     * @param title 문서 제목
     * @param content 문서 내용
     * @param metadata 추가 메타데이터
     * @return 성공 여부
     */
    fun embedSchoolDocument(
        schoolId: UUID,
        docType: String,
        title: String,
        content: String,
        metadata: Map<String, Any> = emptyMap()
    ): Boolean {
        return try {
            // Spring AI Document 생성
            val document = Document(
                content,
                mapOf(
                    "school_id" to schoolId.toString(),
                    "document_type" to docType,
                    "title" to title
                ) + metadata
            )
            
            // VectorStore에 추가 (자동 임베딩 + 저장)
            schoolDocumentVectorStore.add(listOf(document))
            
            logger.info("School document embedded: $title (schoolId=$schoolId, type=$docType)")
            true
        } catch (e: Exception) {
            logger.error("Failed to embed school document: $title", e)
            false
        }
    }
    
    /**
     * 프로그램 문서를 임베딩하여 VectorStore에 저장.
     * 
     * @param programId 프로그램 ID
     * @param docType 문서 타입 (curriculum, career_outcome, student_review)
     * @param title 문서 제목
     * @param content 문서 내용
     * @param metadata 추가 메타데이터
     * @return 성공 여부
     */
    fun embedProgramDocument(
        programId: UUID,
        docType: String,
        title: String,
        content: String,
        metadata: Map<String, Any> = emptyMap()
    ): Boolean {
        return try {
            val document = Document(
                content,
                mapOf(
                    "program_id" to programId.toString(),
                    "document_type" to docType,
                    "title" to title
                ) + metadata
            )
            
            programDocumentVectorStore.add(listOf(document))
            
            logger.info("Program document embedded: $title (programId=$programId, type=$docType)")
            true
        } catch (e: Exception) {
            logger.error("Failed to embed program document: $title", e)
            false
        }
    }
    
    /**
     * 배치 학교 문서 임베딩.
     * 
     * @param documents 문서 목록 (schoolId, docType, title, content, metadata)
     * @return 성공 개수
     */
    fun embedSchoolDocumentsBatch(
        documents: List<SchoolDocumentInput>
    ): Int {
        var successCount = 0
        var failureCount = 0
        
        documents.forEachIndexed { index, doc ->
            if (embedSchoolDocument(doc.schoolId, doc.docType, doc.title, doc.content, doc.metadata)) {
                successCount++
            } else {
                failureCount++
            }
            
            // 진행 상황 로깅
            if ((index + 1) % 10 == 0) {
                logger.info("Progress: ${index + 1}/${documents.size} documents processed")
            }
            
            // Rate limiting (60 req/min)
            if (index < documents.size - 1) {
                Thread.sleep(1000)
            }
        }
        
        logger.info("Batch embedding completed: $successCount succeeded, $failureCount failed")
        return successCount
    }
}

/**
 * 학교 문서 입력 데이터.
 */
data class SchoolDocumentInput(
    val schoolId: UUID,
    val docType: String,
    val title: String,
    val content: String,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * 프로그램 문서 입력 데이터.
 */
data class ProgramDocumentInput(
    val programId: UUID,
    val docType: String,
    val title: String,
    val content: String,
    val metadata: Map<String, Any> = emptyMap()
)
