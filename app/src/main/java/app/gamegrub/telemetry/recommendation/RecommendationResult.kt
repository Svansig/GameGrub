package app.gamegrub.telemetry.recommendation

import kotlinx.serialization.Serializable

/**
 * Result of a recommendation query.
 *
 * Sealed type representing successful recommendations, no-data scenarios,
 * or error conditions when resolving launch configurations.
 *
 * @property titleId The game title being recommended for
 * @property source The source of recommendations (local, curated, fallback)
 */
@Serializable
sealed class RecommendationResult {
    abstract val titleId: String
    abstract val source: RecommendationSource

    @Serializable
    data class Success(
        override val titleId: String,
        override val source: RecommendationSource,
        val recommendations: List<Recommendation>,
        val confidence: Float,
    ) : RecommendationResult()

    @Serializable
    data class NoData(
        override val titleId: String,
        override val source: RecommendationSource = RecommendationSource.CURATED,
        val reason: String,
    ) : RecommendationResult()

    @Serializable
    data class Error(
        override val titleId: String,
        override val source: RecommendationSource = RecommendationSource.CURATED,
        val message: String,
    ) : RecommendationResult()
}

/**
 * A recommended runtime configuration for a game.
 *
 * @property baseId Base bundle identifier
 * @property runtimeId Runtime identifier (Wine/Proton version)
 * @property driverId Driver identifier (optional)
 * @property profileId Launch profile identifier (optional)
 * @property score Confidence score from 0.0 to 1.0
 * @property compatibilityLevel Compatibility classification
 * @property reason Human-readable explanation of this recommendation
 */
@Serializable
data class Recommendation(
    val baseId: String,
    val runtimeId: String,
    val driverId: String?,
    val profileId: String?,
    val score: Float = 1.0f,
    val compatibilityLevel: CompatibilityLevel = CompatibilityLevel.UNKNOWN,
    val reason: String = "",
)

@Serializable
enum class RecommendationSource {
    LOCAL,
    CURATED,
    FALLBACK,
}