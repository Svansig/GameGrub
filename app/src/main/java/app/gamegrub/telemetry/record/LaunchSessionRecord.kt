package app.gamegrub.telemetry.record

import app.gamegrub.session.model.LaunchMilestone
import kotlinx.serialization.Serializable

@Serializable
data class LaunchSessionRecord(
    val sessionId: String,
    val titleId: String,
    val titleName: String,
    val deviceClass: String,
    val baseId: String?,
    val runtimeId: String?,
    val driverId: String?,
    val profileId: String?,
    val outcome: LaunchOutcome,
    val exitCode: Int? = null,
    val exitSignal: String? = null,
    val startTime: Long,
    val endTime: Long,
    val durationMs: Long,
    val milestones: List<SessionMilestone> = emptyList(),
    val fallbackPath: List<String> = emptyList(),
    val errorMessage: String? = null,
    val metadata: Map<String, String> = emptyMap(),
)

@Serializable
enum class LaunchOutcome {
    SUCCESS,
    FAILURE,
    CANCELLED,
    TIMEOUT,
}

@Serializable
data class SessionMilestone(
    val milestone: String,
    val timestamp: Long,
    val metadata: Map<String, String> = emptyMap(),
)

fun createSessionMilestone(milestone: LaunchMilestone, timestamp: Long, metadata: Map<String, String> = emptyMap()): SessionMilestone {
    return SessionMilestone(
        milestone = milestone.name,
        timestamp = timestamp,
        metadata = metadata,
    )
}