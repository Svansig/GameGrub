package app.gamegrub.telemetry.recommendation

import kotlinx.serialization.Serializable

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