package com.goalmond.api.service.graphrag

import com.goalmond.api.domain.entity.GraphRagEntity
import com.goalmond.api.domain.graphrag.EntityType
import com.goalmond.api.repository.GraphRagEntityRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Entity Resolution Service
 * 
 * Entity 중복 제거 및 정규화를 담당합니다.
 * - Canonical name 정규화
 * - Alias 매칭
 * - 신뢰도 점수 기반 병합
 */
@Service
class EntityResolutionService(
    private val entityRepository: GraphRagEntityRepository
) {
    private val logger = LoggerFactory.getLogger(EntityResolutionService::class.java)
    
    /**
     * Entity 이름을 정규화하여 Canonical Name 생성
     * 
     * 규칙:
     * 1. 소문자 변환
     * 2. 앞뒤 공백 제거
     * 3. 연속된 공백을 하나로 축소
     * 4. 특수문자 정리 (& → and, Inc. 제거 등)
     */
    fun normalizeEntityName(name: String): String {
        return name.trim()
            .lowercase()
            .replace(Regex("\\s+"), " ")  // 연속된 공백을 하나로
            .replace("&", "and")
            .replace(Regex("\\bincorporated\\b"), "")  // "Incorporated" 제거
            .replace(Regex("\\binc\\.?\\b"), "")  // "Inc." 또는 "Inc" 제거
            .replace(Regex("\\bcorporation\\b"), "")  // "Corporation" 제거
            .replace(Regex("\\bcorp\\.?\\b"), "")  // "Corp." 또는 "Corp" 제거
            .replace(Regex("\\bllc\\.?\\b"), "")  // "LLC" 제거
            .replace(Regex("\\bltd\\.?\\b"), "")  // "Ltd." 제거
            .replace(Regex("[^a-z0-9\\s-]"), "")  // 알파벳, 숫자, 공백, 하이픈만 남김
            .replace(Regex("\\s+"), " ")  // 제거 후 남은 연속 공백 재정리
            .trim()
    }
    
    /**
     * Alias와 매칭되는 Entity 찾기
     * 
     * @param name Entity 이름
     * @param type Entity 타입
     * @return 매칭된 Entity (없으면 null)
     */
    fun findByAlias(name: String, type: EntityType): GraphRagEntity? {
        val normalizedName = normalizeEntityName(name)
        
        // 1. Canonical name으로 정확 매칭
        val exactMatch = entityRepository.findByEntityTypeAndCanonicalName(type, normalizedName)
        if (exactMatch.isPresent) {
            logger.debug("Found exact match for '{}': {}", name, exactMatch.get().entityName)
            return exactMatch.get()
        }
        
        // 2. Aliases에서 매칭
        val entities = entityRepository.findByEntityType(type)
        for (entity in entities) {
            if (entity.aliases.any { normalizeEntityName(it) == normalizedName }) {
                logger.debug("Found alias match for '{}': {}", name, entity.entityName)
                return entity
            }
        }
        
        return null
    }
    
    /**
     * Entity 유사도 점수 계산 (0.0 ~ 1.0)
     * 
     * - Exact match: 1.0
     * - Alias match: 0.9
     * - Fuzzy match (Levenshtein): 0.5 ~ 0.8
     */
    fun calculateSimilarityScore(entity1: GraphRagEntity, entity2: GraphRagEntity): Double {
        if (entity1.entityType != entity2.entityType) {
            return 0.0
        }
        
        val name1 = entity1.canonicalName
        val name2 = entity2.canonicalName
        
        // Exact match
        if (name1 == name2) {
            return 1.0
        }
        
        // Alias match
        if (entity1.aliases.any { normalizeEntityName(it) == name2 } ||
            entity2.aliases.any { normalizeEntityName(it) == name1 }) {
            return 0.9
        }
        
        // Fuzzy match using Levenshtein distance on suffix-stripped names
        // Strip common institution suffixes to avoid false-positive similarity
        // e.g. "Stanford University" vs "Harvard University" shares "University"
        val stripped1 = stripInstitutionSuffixes(name1)
        val stripped2 = stripInstitutionSuffixes(name2)
        val compare1 = if (stripped1.isNotBlank()) stripped1 else name1
        val compare2 = if (stripped2.isNotBlank()) stripped2 else name2

        // If stripped core names are identical → strong match
        if (compare1 == compare2) return 0.95

        val distance = levenshteinDistance(compare1, compare2)
        val maxLength = maxOf(compare1.length, compare2.length)
        if (maxLength == 0) return 0.0
        val similarity = 1.0 - (distance.toDouble() / maxLength)

        return if (similarity > 0.7) similarity else 0.0
    }

    /**
     * 기관명에서 공통 접미어(university, college 등) 제거
     * Levenshtein 비교 시 false-positive 방지용
     */
    private fun stripInstitutionSuffixes(name: String): String {
        val suffixes = listOf(
            "university", "univ", "college", "institute", "school",
            "academy", "polytechnic", "tech", "technology"
        )
        var result = name
        for (suffix in suffixes) {
            result = result.replace(Regex("\\b$suffix\\b"), "").trim()
        }
        return result.replace(Regex("\\s+"), " ").trim()
    }
    
    /**
     * 중복 Entity 감지 및 병합 후보 반환
     * 
     * @param type Entity 타입
     * @param similarityThreshold 유사도 임계값 (기본 0.85)
     * @return 병합 후보 Entity 쌍 리스트
     */
    fun findDuplicateCandidates(
        type: EntityType,
        similarityThreshold: Double = 0.85
    ): List<Pair<GraphRagEntity, GraphRagEntity>> {
        val entities = entityRepository.findByEntityType(type)
        val duplicates = mutableListOf<Pair<GraphRagEntity, GraphRagEntity>>()
        
        for (i in entities.indices) {
            for (j in i + 1 until entities.size) {
                val score = calculateSimilarityScore(entities[i], entities[j])
                if (score >= similarityThreshold) {
                    duplicates.add(Pair(entities[i], entities[j]))
                    logger.info(
                        "Found duplicate candidate: '{}' <-> '{}' (score: {})",
                        entities[i].entityName,
                        entities[j].entityName,
                        score
                    )
                }
            }
        }
        
        return duplicates
    }
    
    /**
     * Levenshtein Distance 계산 (편집 거리)
     */
    private fun levenshteinDistance(str1: String, str2: String): Int {
        val dp = Array(str1.length + 1) { IntArray(str2.length + 1) }
        
        for (i in 0..str1.length) {
            dp[i][0] = i
        }
        for (j in 0..str2.length) {
            dp[0][j] = j
        }
        
        for (i in 1..str1.length) {
            for (j in 1..str2.length) {
                val cost = if (str1[i - 1] == str2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }
        
        return dp[str1.length][str2.length]
    }
    
    /**
     * Entity 신뢰도 점수 재계산
     * 
     * 고려 요소:
     * - Source URL 개수
     * - 연결된 Triple 개수
     * - Extraction method (LLM vs Manual)
     */
    fun recalculateConfidenceScore(entity: GraphRagEntity, tripleCount: Int): Double {
        var score = entity.confidenceScore.toDouble()
        
        // Source URL 보너스 (최대 +0.1)
        val sourceBonus = minOf(entity.sourceUrls.size * 0.02, 0.1)
        score += sourceBonus
        
        // Triple 연결 보너스 (최대 +0.15)
        val tripleBonus = minOf(tripleCount * 0.03, 0.15)
        score += tripleBonus
        
        return minOf(score, 1.0)
    }
}
