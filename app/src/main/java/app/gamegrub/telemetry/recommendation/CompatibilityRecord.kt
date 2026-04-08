package app.gamegrub.telemetry.recommendation

import app.gamegrub.telemetry.record.LaunchOutcome
import app.gamegrub.telemetry.record.LaunchSessionRecord
import kotlinx.serialization.Serializable

/**
 * Compatibility levels for game launches.
 *
 * Used to classify and score launch configurations based on historical
 * success data and session duration.
 */
@Serializable
enum class CompatibilityLevel {
    EXCELLENT,
    GOOD,
    FAIR,
    POOR,
    UNKNOWN,
}

/**
 * Compatibility record derived from a launch session record.
 *
 * Enriches the raw launch data with computed compatibility level
 * and aggregated statistics for recommendation processing.
 *
 * @property sessionId The session this record is based on
 * @property titleId Platform-specific game identifier
 * @property titleName Display name of the game
 * @property deviceClass Device classification
 * @property baseId Base bundle identifier
 * @property runtimeId Runtime identifier
 * @property driverId Driver identifier
 * @property profileId Profile identifier
 * @property outcome Launch outcome
 * @property exitCode Process exit code
 * @property exitSignal Signal if killed
 * @property startTime Session start time
 * @property endTime Session end time
 * @property durationMs Session duration
 * @property milestones Session milestones
 * @property fallbackPath Fallback configurations tried
 * @property errorMessage Error message if failed
 * @property metadata Additional context
 * @property compatibilityLevel Computed compatibility classification
 * @property runsCompleted Number of successful runs with this config
 * @property lastPlayed Unix timestamp of last successful play
 */
@Serializable
data class CompatibilityRecord(
    val sessionId: String,
    val titleId: String,
    val titleName: String,
    val deviceClass: String,
    val baseId: String?,
    val runtimeId: String?,
    val driverId: String?,
    val profileId: String?,
    val outcome: LaunchOutcome,
    val exitCode: Int?,
    val exitSignal: String?,
    val startTime: Long,
    val endTime: Long,
    val durationMs: Long,
    val milestones: List<app.gamegrub.telemetry.record.SessionMilestone>,
    val fallbackPath: List<String>,
    val errorMessage: String?,
    val metadata: Map<String, String>,
    val compatibilityLevel: CompatibilityLevel = CompatibilityLevel.UNKNOWN,
    val runsCompleted: Int = 1,
    val lastPlayed: Long = System.currentTimeMillis(),
) {
    companion object {
        fun fromLaunchRecord(record: LaunchSessionRecord): CompatibilityRecord {
            val compatibilityLevel = when {
                record.outcome == LaunchOutcome.SUCCESS && record.durationMs > 300_000 -> CompatibilityLevel.EXCELLENT
                record.outcome == LaunchOutcome.SUCCESS && record.durationMs > 60_000 -> CompatibilityLevel.GOOD
                record.outcome == LaunchOutcome.SUCCESS -> CompatibilityLevel.FAIR
                record.outcome == LaunchOutcome.FAILURE -> CompatibilityLevel.POOR
                else -> CompatibilityLevel.UNKNOWN
            }

            return CompatibilityRecord(
                sessionId = record.sessionId,
                titleId = record.titleId,
                titleName = record.titleName,
                deviceClass = record.deviceClass,
                baseId = record.baseId,
                runtimeId = record.runtimeId,
                driverId = record.driverId,
                profileId = record.profileId,
                outcome = record.outcome,
                exitCode = record.exitCode,
                exitSignal = record.exitSignal,
                startTime = record.startTime,
                endTime = record.endTime,
                durationMs = record.durationMs,
                milestones = record.milestones,
                fallbackPath = record.fallbackPath,
                errorMessage = record.errorMessage,
                metadata = record.metadata,
                compatibilityLevel = compatibilityLevel,
            )
        }
    }
}
