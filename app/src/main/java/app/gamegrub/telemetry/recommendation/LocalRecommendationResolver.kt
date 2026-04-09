package app.gamegrub.telemetry.recommendation

import app.gamegrub.telemetry.record.LaunchOutcome
import app.gamegrub.telemetry.record.LaunchRecordStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Resolver that provides recommendations based on local launch history.
 *
 * Queries LaunchRecordStore to find successful launch configurations
 * and returns them as ranked recommendations. Uses last-known-good
 * pattern for primary recommendations.
 */
@Singleton
class LocalRecommendationResolver @Inject constructor(
    private val recordStore: LaunchRecordStore,
) {
    suspend fun getLastKnownGood(titleId: String): Recommendation? = withContext(Dispatchers.IO) {
        val records = recordStore.getRecordsByTitle(titleId)
            .filter { it.outcome == LaunchOutcome.SUCCESS }
            .sortedByDescending { it.endTime }

        if (records.isEmpty()) {
            Timber.d("No successful records found for title: $titleId")
            return@withContext null
        }

        val best = records.first()
        val compatibility = CompatibilityRecord.fromLaunchRecord(best)

        val score = when (compatibility.compatibilityLevel) {
            CompatibilityLevel.EXCELLENT -> 1.0f
            CompatibilityLevel.GOOD -> 0.8f
            CompatibilityLevel.FAIR -> 0.6f
            CompatibilityLevel.POOR -> 0.4f
            CompatibilityLevel.UNKNOWN -> 0.2f
        }

        Timber.d("Found LKG for $titleId: runtime=${best.runtimeId}, driver=${best.driverId}")

        Recommendation(
            baseId = best.baseId ?: "",
            runtimeId = best.runtimeId ?: "",
            driverId = best.driverId,
            profileId = best.profileId,
            score = score,
            compatibilityLevel = compatibility.compatibilityLevel,
            reason = "Local last known good from ${records.size} successful runs",
        )
    }

    suspend fun getRecommendations(titleId: String, limit: Int = 5): List<Recommendation> = withContext(Dispatchers.IO) {
        val records = recordStore.getRecordsByTitle(titleId)
            .filter { it.outcome == LaunchOutcome.SUCCESS }
            .sortedByDescending { it.endTime }
            .take(limit)

        records.map { record ->
            val compatibility = CompatibilityRecord.fromLaunchRecord(record)
            val score = when (compatibility.compatibilityLevel) {
                CompatibilityLevel.EXCELLENT -> 1.0f
                CompatibilityLevel.GOOD -> 0.8f
                CompatibilityLevel.FAIR -> 0.6f
                CompatibilityLevel.POOR -> 0.4f
                CompatibilityLevel.UNKNOWN -> 0.2f
            }

            Recommendation(
                baseId = record.baseId ?: "",
                runtimeId = record.runtimeId ?: "",
                driverId = record.driverId,
                profileId = record.profileId,
                score = score,
                compatibilityLevel = compatibility.compatibilityLevel,
                reason = "Successful run on ${
                    java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(record.endTime))
                }",
            )
        }
    }

    suspend fun getAllKnownConfigurations(titleId: String): Set<Recommendation> = withContext(Dispatchers.IO) {
        val records = recordStore.getRecordsByTitle(titleId)
            .filter { it.outcome == LaunchOutcome.SUCCESS }

        records.mapNotNull { record ->
            if (record.baseId == null || record.runtimeId == null) return@mapNotNull null

            Recommendation(
                baseId = record.baseId,
                runtimeId = record.runtimeId,
                driverId = record.driverId,
                profileId = record.profileId,
                score = 0.5f,
                compatibilityLevel = CompatibilityLevel.UNKNOWN,
            )
        }.toSet()
    }
}
