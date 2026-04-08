package app.gamegrub.telemetry.record

import app.gamegrub.telemetry.session.LaunchMilestone
import kotlinx.serialization.Serializable

/**
 * Persisted record of a completed game launch session.
 *
 * Captures the complete outcome of a launch attempt including timing,
 * configuration, milestones, and any error information. Used for
 * telemetry analysis, recommendation systems, and debugging.
 *
 * @property sessionId Unique identifier for this session
 * @property titleId Platform-specific game identifier (appId, productId, etc.)
 * @property titleName Display name of the game
 * @property deviceClass Device classification (e.g., "SM8550")
 * @property baseId The base bundle identifier used
 * @property runtimeId The runtime identifier used
 * @property driverId The driver identifier used
 * @property profileId The launch profile identifier used
 * @property outcome The final result of the launch
 * @property exitCode Process exit code if available
 * @property exitSignal Signal name if process was killed
 * @property startTime Unix epoch milliseconds when launch started
 * @property endTime Unix epoch milliseconds when launch ended
 * @property durationMs Total launch duration in milliseconds
 * @property milestones List of recorded milestones with timestamps
 * @property fallbackPath List of fallback configurations attempted
 * @property errorMessage Error message if launch failed
 * @property metadata Additional context key-value pairs
 */
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

/**
 * Possible outcomes of a game launch attempt.
 */
@Serializable
enum class LaunchOutcome {
    SUCCESS,
    FAILURE,
    CANCELLED,
    TIMEOUT,
}

/**
 * A milestone event within a launch session.
 *
 * @property milestone Name of the milestone
 * @property timestamp Unix epoch milliseconds when it occurred
 * @property metadata Additional context for this event
 */
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
