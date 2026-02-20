package com.goalmond.api.service.matching

import com.goalmond.api.service.graphrag.CareerPath
import com.goalmond.api.service.graphrag.ProgramSearchResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class HybridRankingService {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun rankCandidates(
        candidates: List<MatchingCandidate>,
        vectorScoreMap: Map<UUID, Double>,
        careerPaths: List<CareerPath>,
        skillProgramMatches: List<ProgramSearchResult>,
        detectedSkills: List<String>,
        topN: Int = 5
    ): List<MatchingCandidate> {
        if (candidates.isEmpty()) return emptyList()

        val maxGraphWeight = careerPaths.maxOfOrNull { it.weight } ?: 1.0
        val maxSkillRelevance = skillProgramMatches.maxOfOrNull { it.relevanceScore } ?: 1.0
        val programSkillScoreMap = buildProgramSkillScoreMap(skillProgramMatches, maxSkillRelevance)

        val enriched = candidates.map { candidate ->
            val schoolId = candidate.school.id
            val vectorScore = schoolId?.let { vectorScoreMap[it] } ?: 0.0
            val graphPath = selectGraphPath(candidate, careerPaths)
            val pathGraphScore = computeGraphScore(graphPath, maxGraphWeight)
            val skillGraphScore = candidate.program.id?.let { programSkillScoreMap[it] } ?: 0.0
            val graphScore = (
                (pathGraphScore * GRAPH_PATH_SIGNAL_WEIGHT) +
                    (skillGraphScore * GRAPH_SKILL_SIGNAL_WEIGHT)
                ).coerceIn(0.0, 1.0)
            val preferenceNormalized = (candidate.scores.career / MAX_CAREER_SCORE).coerceIn(0.0, 1.0)
            val hybridNormalized = (vectorScore * VECTOR_WEIGHT) +
                (graphScore * GRAPH_WEIGHT) +
                (preferenceNormalized * PREFERENCE_WEIGHT)
            val rankingScore = (hybridNormalized * 100).coerceIn(0.0, 100.0)

            candidate.copy(
                vectorScore = vectorScore,
                graphScore = graphScore,
                graphPath = graphPath,
                rankingScore = rankingScore
            )
        }

        val sorted = enriched
            .sortedWith(
                compareByDescending<MatchingCandidate> { it.rankingScore }
                    .thenByDescending { it.graphScore }
                    .thenByDescending { it.vectorScore }
                    .thenByDescending { it.totalScore }
                    .thenBy { it.school.name }
            )
            .take(topN)

        logHybridDiagnostics(sorted, detectedSkills)
        return sorted
    }

    private fun buildProgramSkillScoreMap(matches: List<ProgramSearchResult>, maxSkillRelevance: Double): Map<UUID, Double> {
        if (matches.isEmpty() || maxSkillRelevance <= 0.0) return emptyMap()

        return matches.mapNotNull { result ->
            val programId = result.programId ?: return@mapNotNull null
            val normalizedScore = (result.relevanceScore / maxSkillRelevance).coerceIn(0.0, 1.0)
            programId to normalizedScore
        }.toMap()
    }

    private fun selectGraphPath(candidate: MatchingCandidate, paths: List<CareerPath>): CareerPath? {
        val schoolId = candidate.school.id ?: return null
        val matchingPaths = paths.filter { it.schoolId == schoolId }
        if (matchingPaths.isEmpty()) return null
        val programMatch = candidate.program.id?.let { pid ->
            matchingPaths.firstOrNull { it.programId == pid }
        }
        return programMatch ?: matchingPaths.maxByOrNull { it.weight }
    }

    private fun computeGraphScore(path: CareerPath?, maxWeight: Double): Double {
        if (path == null || maxWeight <= 0.0) return 0.0
        return (path.weight / maxWeight).coerceIn(0.0, 1.0)
    }

    private fun logHybridDiagnostics(topCandidates: List<MatchingCandidate>, detectedSkills: List<String>) {
        if (topCandidates.isEmpty()) {
            logger.info("Hybrid diagnostics: no top candidates")
            return
        }

        val graphPathCount = topCandidates.count { it.graphPath != null }
        val coverage = graphPathCount.toDouble() / topCandidates.size.toDouble()
        val avgVector = topCandidates.map { it.vectorScore }.average()
        val avgGraph = topCandidates.map { it.graphScore }.average()
        val avgPreference = topCandidates
            .map { (it.scores.career / MAX_CAREER_SCORE).coerceIn(0.0, 1.0) }
            .average()

        logger.info(
            "Hybrid diagnostics: topN={}, graphPathCoverage={}/{}, detectedSkills={}, avgVector={}, avgGraph={}, avgPreference={}",
            topCandidates.size,
            graphPathCount,
            topCandidates.size,
            detectedSkills,
            String.format("%.4f", avgVector),
            String.format("%.4f", avgGraph),
            String.format("%.4f", avgPreference)
        )

        topCandidates.forEachIndexed { index, candidate ->
            logger.debug(
                "Hybrid rank {}: school='{}', program='{}', rankingScore={}, vectorScore={}, graphScore={}, totalScore={}, hasGraphPath={}",
                index + 1,
                candidate.school.name,
                candidate.program.name,
                String.format("%.2f", candidate.rankingScore),
                String.format("%.4f", candidate.vectorScore),
                String.format("%.4f", candidate.graphScore),
                String.format("%.2f", candidate.totalScore),
                candidate.graphPath != null
            )
        }

        if (coverage < GRAPH_PATH_COVERAGE_ALERT_THRESHOLD) {
            logger.warn(
                "Hybrid diagnostics alert: graph path coverage below threshold (coverage={}, threshold={})",
                String.format("%.2f", coverage),
                GRAPH_PATH_COVERAGE_ALERT_THRESHOLD
            )
        }
    }

    companion object {
        const val VECTOR_WEIGHT = 0.4
        const val GRAPH_WEIGHT = 0.5
        const val PREFERENCE_WEIGHT = 0.1
        const val GRAPH_PATH_SIGNAL_WEIGHT = 0.7
        const val GRAPH_SKILL_SIGNAL_WEIGHT = 0.3
        const val MAX_CAREER_SCORE = 30.0
        const val GRAPH_PATH_COVERAGE_ALERT_THRESHOLD = 0.4
    }
}
