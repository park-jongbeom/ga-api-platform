package com.goalmond.api.service.graphrag

import com.goalmond.api.config.EntityResolutionProperties
import com.goalmond.api.domain.entity.GraphRagEntity
import com.goalmond.api.domain.graphrag.EntityType
import com.goalmond.api.repository.GraphRagEntityRepository
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Duration
import java.util.UUID

/**
 * Entity Resolution Service
 *
 * Entity 중복 제거 및 정규화를 담당합니다.
 */
@Service
class EntityResolutionService(
    private val entityRepository: GraphRagEntityRepository,
    private val properties: EntityResolutionProperties,
    @Qualifier("entityAliasDictionary")
    private val aliasDictionary: Map<String, List<String>>
) {
    private val logger = LoggerFactory.getLogger(EntityResolutionService::class.java)

    private val aliasToCanonicalLookup: Map<String, String>
    private val canonicalAliasLookup: Map<String, List<String>>
    private val cacheDuration = Duration.ofMinutes(properties.cacheTtlMinutes)
    private val entityListCache: LoadingCache<EntityType, List<GraphRagEntity>>
    private val aliasIndexCache: LoadingCache<EntityType, Map<String, GraphRagEntity>>

    init {
        val canonicalBuilder = mutableMapOf<String, MutableList<String>>()
        val aliasBuilder = mutableMapOf<String, String>()

        aliasDictionary.forEach { (canonical, aliases) ->
            val canonicalNormalized = basicNormalize(canonical)
            aliasBuilder[canonicalNormalized] = canonicalNormalized
            val normalizedAliases = aliases.map { basicNormalize(it) }.filter { it.isNotBlank() }
            normalizedAliases.forEach { aliasNormalized ->
                aliasBuilder[aliasNormalized] = canonicalNormalized
            }
            canonicalBuilder.computeIfAbsent(canonicalNormalized) { mutableListOf() }
                .addAll(normalizedAliases)
        }

        canonicalAliasLookup = canonicalBuilder.mapValues { it.value.distinct() }
        aliasBuilder.putAll(canonicalAliasLookup.keys.associateWith { it })
        aliasToCanonicalLookup = aliasBuilder.toMap()

        entityListCache = Caffeine.newBuilder()
            .maximumSize(properties.cacheSize.toLong())
            .expireAfterWrite(cacheDuration)
            .build { entityRepository.findByEntityType(it) }

        aliasIndexCache = Caffeine.newBuilder()
            .maximumSize(properties.cacheSize.toLong())
            .expireAfterWrite(cacheDuration)
            .build { type -> buildAliasIndex(entityListCache.get(type)) }
    }

    /**
     * Entity 이름을 정규화하여 Canonical Name 생성
     */
    fun normalizeEntityName(name: String): String {
        val result = basicNormalize(name)
        return aliasToCanonicalLookup[result] ?: result
    }

    /**
     * Alias와 매칭되는 Entity 찾기
     */
    fun findByAlias(name: String, type: EntityType): GraphRagEntity? {
        val normalizedName = normalizeEntityName(name)
        val aliasMap = aliasIndexCache.get(type)
        val found = aliasMap[normalizedName]
        if (found != null) {
            logger.debug("Alias match for '{}' -> {}", name, found.entityName)
        }
        return found
    }

    /**
     * Entity 유사도 점수 계산 (0.0 ~ 1.0)
     */
    fun calculateSimilarityScore(entity1: GraphRagEntity, entity2: GraphRagEntity): Double {
        if (entity1.entityType != entity2.entityType) {
            return 0.0
        }

        val name1 = normalizeEntityName(entity1.canonicalName)
        val name2 = normalizeEntityName(entity2.canonicalName)

        if (name1 == name2) {
            return 1.0
        }

        if (entity1.aliases.any { normalizeEntityName(it) == name2 } ||
            entity2.aliases.any { normalizeEntityName(it) == name1 }) {
            return 0.9
        }

        val stripped1 = stripInstitutionSuffixes(name1)
        val stripped2 = stripInstitutionSuffixes(name2)
        val compare1 = if (stripped1.isNotBlank()) stripped1 else name1
        val compare2 = if (stripped2.isNotBlank()) stripped2 else name2

        if (compare1 == compare2) return 0.95

        val distance = levenshteinDistance(compare1, compare2)
        val maxLength = maxOf(compare1.length, compare2.length)
        if (maxLength == 0) return 0.0
        val similarity = 1.0 - (distance.toDouble() / maxLength)

        return if (similarity > 0.7) similarity else 0.0
    }

    /**
     * 중복 Entity 감지 및 병합 후보 반환
     */
    fun findDuplicateCandidates(
        type: EntityType,
        similarityThreshold: Double = properties.similarityThreshold
    ): List<Pair<GraphRagEntity, GraphRagEntity>> {
        val entities = entityListCache.get(type)
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
     * Entity 신뢰도 점수 재계산
     */
    fun recalculateConfidenceScore(entity: GraphRagEntity, tripleCount: Int): Double {
        var score = entity.confidenceScore.toDouble()

        val sourceBonus = minOf(entity.sourceUrls.size * 0.02, 0.1)
        score += sourceBonus

        val tripleBonus = minOf(tripleCount * 0.03, 0.15)
        score += tripleBonus

        return minOf(score, 1.0)
    }

    /**
     * 이름/타입으로 Entity를 찾거나 신규 등록합니다.
     */
    @Transactional
    fun resolveOrCreateEntity(
        name: String,
        type: EntityType,
        aliases: List<String> = emptyList(),
        metadata: Map<String, Any> = emptyMap(),
        schoolId: UUID? = null,
        programId: UUID? = null,
        confidenceScore: Double? = null,
        sourceUrls: Array<String> = emptyArray()
    ): GraphRagEntity {
        findByAlias(name, type)?.let { return it }
        val canonical = normalizeEntityName(name)
        val created = persistNewEntity(
            name = name.trim(),
            canonicalName = canonical,
            type = type,
            metadata = metadata,
            aliases = aliases,
            schoolId = schoolId,
            programId = programId,
            confidenceScore = confidenceScore ?: properties.defaultConfidenceScore,
            sourceUrls = sourceUrls
        )
        refreshCachesForType(type)
        return created
    }

    /**
     * 배치 엔티티 정규화 및 등록 (1000개 이상 처리 목표)
     */
    @Transactional
    fun resolveEntitiesBatch(rawNames: List<String>, type: EntityType): List<GraphRagEntity> {
        if (rawNames.isEmpty()) {
            return emptyList()
        }

        val aliasIndex = aliasIndexCache.get(type).toMutableMap()
        val resolvedEntities = mutableListOf<GraphRagEntity>()
        var cacheInvalidated = false

        rawNames.forEach { raw ->
            val normalized = normalizeEntityName(raw)
            val existing = aliasIndex[normalized]
            if (existing != null) {
                resolvedEntities.add(existing)
                return@forEach
            }

            val created = persistNewEntity(
                name = raw.trim(),
                canonicalName = normalized,
                type = type,
                metadata = emptyMap(),
                aliases = emptyList(),
                schoolId = null,
                programId = null,
                confidenceScore = properties.defaultConfidenceScore,
                sourceUrls = emptyArray()
            )

            addEntityToAliasIndex(created, aliasIndex)
            resolvedEntities.add(created)
            cacheInvalidated = true
        }

        if (cacheInvalidated) {
            refreshCachesForType(type)
        }

        return resolvedEntities
    }

    private fun refreshCachesForType(type: EntityType) {
        entityListCache.invalidate(type)
        aliasIndexCache.invalidate(type)
    }

    private fun buildAliasIndex(entities: List<GraphRagEntity>): Map<String, GraphRagEntity> {
        val index = mutableMapOf<String, GraphRagEntity>()
        entities.forEach { entity -> addEntityToAliasIndex(entity, index) }
        return index
    }

    private fun addEntityToAliasIndex(entity: GraphRagEntity, index: MutableMap<String, GraphRagEntity>) {
        val canonicalNormalized = normalizeEntityName(entity.canonicalName)
        index[canonicalNormalized] = entity
        entity.aliases.forEach { alias ->
            val normalizedAlias = basicNormalize(alias)
            if (normalizedAlias.isNotBlank()) {
                index[normalizedAlias] = entity
            }
        }
        canonicalAliasLookup[canonicalNormalized]?.forEach { dictionaryAlias ->
            if (dictionaryAlias.isNotBlank()) {
                index[dictionaryAlias] = entity
            }
        }
    }

    private fun persistNewEntity(
        name: String,
        canonicalName: String,
        type: EntityType,
        metadata: Map<String, Any>,
        aliases: List<String>,
        schoolId: UUID?,
        programId: UUID?,
        confidenceScore: Double,
        sourceUrls: Array<String>
    ): GraphRagEntity {
        val clampedScore = confidenceScore.coerceIn(0.0, 1.0)
        val newEntity = GraphRagEntity(
            entityType = type,
            entityName = name,
            canonicalName = canonicalName,
            aliases = collectAliasCandidates(canonicalName, aliases),
            metadata = metadata,
            schoolId = schoolId,
            programId = programId,
            confidenceScore = BigDecimal.valueOf(clampedScore),
            sourceUrls = sourceUrls
        )

        return try {
            entityRepository.save(newEntity)
        } catch (ex: DataIntegrityViolationException) {
            logger.warn("Entity already exists during persist: {} / {}", type, canonicalName)
            entityRepository.findByEntityTypeAndCanonicalName(type, canonicalName)
                .orElseThrow { ex }
        }
    }

    private fun collectAliasCandidates(canonicalNormalized: String, userAliases: List<String>): List<String> {
        val aliasSet = linkedSetOf<String>()
        canonicalAliasLookup[canonicalNormalized].orEmpty().forEach { alias ->
            if (alias.isNotBlank()) aliasSet += alias
        }
        userAliases.map { basicNormalize(it) }.filter { alias -> alias.isNotBlank() }
            .forEach { aliasSet += it }
        return aliasSet.toList()
    }

    private fun basicNormalize(name: String): String {
        return name.trim()
            .lowercase()
            .replace(Regex("\\s+"), " ")
            .replace("&", "and")
            .replace(Regex("\\bincorporated\\b"), "")
            .replace(Regex("\\binc\\.?\\b"), "")
            .replace(Regex("\\bcorporation\\b"), "")
            .replace(Regex("\\bcorp\\.?\\b"), "")
            .replace(Regex("\\bllc\\.?\\b"), "")
            .replace(Regex("\\bltd\\.?\\b"), "")
            .replace(Regex("[^a-z0-9\\s-]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

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
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }

        return dp[str1.length][str2.length]
    }
}
