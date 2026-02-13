package com.goalmond.api.service.matching

import com.goalmond.api.domain.entity.AcademicProfile
import com.goalmond.api.domain.entity.School
import com.goalmond.api.domain.entity.User
import com.goalmond.api.domain.entity.UserPreference
import com.goalmond.api.repository.SchoolEmbeddingRepository
import com.goalmond.api.repository.SchoolRepository
import com.goalmond.api.service.ai.GeminiClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

/**
 * 벡터 검색 서비스 (GAM-3, Phase 3).
 * 
 * 사용자 쿼리를 임베딩하여 pgvector 코사인 유사도 검색으로 Top K 학교를 추출합니다.
 */
@Service
class VectorSearchService(
    private val geminiClient: GeminiClient,
    private val schoolEmbeddingRepository: SchoolEmbeddingRepository,
    private val schoolRepository: SchoolRepository,
    @Value("\${app.matching.vector-search.top-k:20}")
    private val topK: Int
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    /**
     * 사용자 프로필을 쿼리 텍스트로 변환.
     * 
     * 포함 정보:
     * - 전공 희망, 목표 프로그램
     * - 희망 지역, 예산
     * - 커리어 목표
     * - GPA, 영어 점수
     */
    fun buildUserQuery(
        user: User,
        profile: AcademicProfile,
        preference: UserPreference
    ): String {
        return buildString {
            appendLine("학생 프로필:")
            preference.targetMajor?.let { appendLine("- 전공 희망: $it") }
            preference.targetProgram?.let { appendLine("- 목표 프로그램: $it") }
            preference.targetLocation?.let { appendLine("- 희망 지역: $it") }
            preference.budgetUsd?.let { appendLine("- 예산: $$it/년") }
            preference.careerGoal?.let { appendLine("- 커리어 목표: $it") }
            profile.gpa?.let { appendLine("- GPA: $it / ${profile.gpaScale}") }
            profile.englishScore?.let { 
                appendLine("- 영어 점수: $it (${profile.englishTestType})") 
            }
            preference.preferredTrack?.let { appendLine("- 선호 트랙: $it") }
        }.trim()
    }
    
    /**
     * 사용자와 유사한 Top K 학교 검색.
     * 
     * @param user 사용자
     * @param profile 학력 프로필
     * @param preference 선호도
     * @return Top K 학교 목록 (유사도 내림차순)
     */
    @Cacheable(value = ["vectorSearch"], key = "#user.id")
    fun searchSimilarSchools(
        user: User,
        profile: AcademicProfile,
        preference: UserPreference
    ): List<VectorSearchCandidate> {
        val startTime = System.currentTimeMillis()
        
        try {
            // 1. 사용자 쿼리 텍스트 생성
            val queryText = buildUserQuery(user, profile, preference)
            logger.debug("User query text:\n$queryText")
            
            // 2. 쿼리 임베딩
            val queryEmbedding = geminiClient.embedContent(queryText)
            val queryVector = queryEmbedding.joinToString(prefix = "[", postfix = "]")
            
            // 3. pgvector 코사인 유사도 검색
            val searchResults = schoolEmbeddingRepository.findTopByCosineSimilarity(
                queryEmbedding = queryVector,
                limit = topK
            )
            
            // 4. School 엔티티 조회
            val schoolIds = searchResults.map { it.getSchoolId() }
            val schools = schoolRepository.findAllById(schoolIds)
            
            // 5. 유사도 순서 유지 (검색 결과 순서대로 정렬)
            val schoolMap = schools.associateBy { it.id }
            val sortedCandidates = searchResults.mapNotNull { result ->
                val school = schoolMap[result.getSchoolId()] ?: return@mapNotNull null
                VectorSearchCandidate(
                    school = school,
                    similarity = result.getSimilarity()
                )
            }
            
            val elapsed = System.currentTimeMillis() - startTime
            logger.info(
                "Vector search completed: ${sortedCandidates.size} schools found in ${elapsed}ms " +
                "(topK=$topK, user=${user.id})"
            )
            
            // 유사도 로깅 (상위 5개)
            sortedCandidates.take(5).forEachIndexed { index, candidate ->
                logger.debug(
                    "Top ${index + 1}: ${candidate.school.name} " +
                    "(similarity=${String.format("%.4f", candidate.similarity)})"
                )
            }
            
            return sortedCandidates
            
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startTime
            logger.error("Vector search failed after ${elapsed}ms", e)
            throw VectorSearchException("Vector search failed for user ${user.id}", e)
        }
    }
}

/**
 * 벡터 검색 예외.
 */
class VectorSearchException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * 벡터 검색 결과 후보.
 */
data class VectorSearchCandidate(
    val school: School,
    val similarity: Double
)
