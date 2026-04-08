package app.gamegrub.telemetry.recommendation

import app.gamegrub.telemetry.record.LaunchSessionRecord
import app.gamegrub.telemetry.record.LaunchOutcome
import kotlinx.serialization.Serializable

@Serializable
enum class CompatibilityLevel {
    EXCELLENT,
    GOOD,
    FAIR,
    POOR,
    UNKNOWN,
}

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