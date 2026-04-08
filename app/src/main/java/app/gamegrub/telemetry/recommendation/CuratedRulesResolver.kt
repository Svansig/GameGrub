package app.gamegrub.telemetry.recommendation

import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * Resolver providing curated recommendations for known games.
 *
 * Contains hardcoded rules for games with known compatibility issues
 * or special requirements. Used as a fallback when local history
 * is insufficient.
 *
 * @see LocalRecommendationResolver
 */
@Singleton
class CuratedRulesResolver @Inject constructor() {

    private val gameRules = mapOf(
        "elden_ring" to GameRule(
            recommendedRuntime = "proton-9.0-arm64ec",
            recommendedDriver = "turnip-merged-2024-03-01",
            notes = "Proton 9.0+ recommended for best performance",
            knownIssues = emptyList(),
        ),
        "cyberpunk_2077" to GameRule(
            recommendedRuntime = "proton-9.0-arm64ec",
            recommendedDriver = "turnip-merged-2024-03-01",
            notes = "Requires Vulkan, use Turnip driver",
            knownIssues = listOf("May need DXVK async"),
        ),
        "baldurs_gate_3" to GameRule(
            recommendedRuntime = "proton-8.0-arm64ec",
            recommendedDriver = "mesa-24.0",
            notes = "Stable with Proton 8.0",
            knownIssues = emptyList(),
        ),
    )

    private val defaultRule = GameRule(
        recommendedRuntime = "wine-8.0-glibc2.35",
        recommendedDriver = null,
        notes = "Default fallback configuration",
        knownIssues = emptyList(),
    )

    fun getRule(titleId: String): GameRule {
        val normalized = titleId.lowercase().replace(" ", "_").replace("-", "_")
        return gameRules[normalized] ?: defaultRule
    }

    fun hasRule(titleId: String): Boolean {
        val normalized = titleId.lowercase().replace(" ", "_").replace("-", "_")
        return gameRules.containsKey(normalized)
    }

    fun getRecommendedConfigurations(titleId: String): List<Recommendation> {
        val rule = getRule(titleId)
        val recommendations = mutableListOf<Recommendation>()

        recommendations.add(
            Recommendation(
                baseId = "base-linux-glibc2.35-2.35",
                runtimeId = rule.recommendedRuntime,
                driverId = rule.recommendedDriver,
                profileId = null,
                score = 1.0f,
                compatibilityLevel = CompatibilityLevel.EXCELLENT,
                reason = rule.notes,
            ),
        )

        if (rule.recommendedDriver == null) {
            recommendations.add(
                Recommendation(
                    baseId = "base-linux-glibc2.35-2.35",
                    runtimeId = rule.recommendedRuntime,
                    driverId = "turnip-merged-2024-03-01",
                    profileId = null,
                    score = 0.8f,
                    compatibilityLevel = CompatibilityLevel.GOOD,
                    reason = "Alternative: with Turnip driver",
                ),
            )
        }

        return recommendations
    }

    fun getAllKnownGames(): Set<String> = gameRules.keys

    companion object {
        val DEFAULT_RUNTIME = "wine-8.0-glibc2.35"
        val DEFAULT_BASE = "base-linux-glibc2.35-2.35"
    }
}

data class GameRule(
    val recommendedRuntime: String,
    val recommendedDriver: String?,
    val notes: String,
    val knownIssues: List<String>,
)
