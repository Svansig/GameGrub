package app.gamegrub.telemetry.session

import kotlinx.serialization.Serializable

/**
 * Milestones representing key stages in the game launch pipeline.
 *
 * These markers provide fine-grained timing and sequencing information
 * for telemetry, debugging, and performance analysis.
 *
 * @see MilestoneRecorder
 * @see MilestoneEmitter
 */
@Serializable
enum class LaunchMilestone {
    LAUNCH_REQUEST_QUEUED,
    ASSEMBLY_START,
    ASSEMBLY_COMPLETE,
    BUNDLE_VERIFICATION_COMPLETE,
    CONTAINER_READY,
    PROCESS_SPAWNED,
    BACKEND_INITIALIZED,
    FIRST_FRAME_RENDERED,
    GAME_INTERACTIVE,
    LAUNCH_TIMEOUT,
    LAUNCH_FAILED,
}

/**
 * A single recorded milestone event.
 *
 * @property sessionId The launch session this milestone belongs to
 * @property milestone The milestone that was reached
 * @property timestamp Unix epoch milliseconds when the milestone was recorded
 * @property metadata Additional context about this milestone event
 */
@Serializable
data class MilestoneRecord(
    val sessionId: String,
    val milestone: LaunchMilestone,
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: Map<String, String> = emptyMap(),
)

/**
 * Records milestones for a single launch session.
 *
 * Provides in-memory recording of launch milestones with timing information.
 * Use MilestoneEmitter for global singleton access.
 *
 * @see LaunchMilestone
 * @see MilestoneEmitter
 */
class MilestoneRecorder {
    private val milestones = mutableListOf<MilestoneRecord>()
    private var sessionId: String? = null

    fun startSession(sessionId: String) {
        this.sessionId = sessionId
        milestones.clear()
    }

    fun record(
        milestone: LaunchMilestone,
        metadata: Map<String, String> = emptyMap(),
    ) {
        val sid = sessionId ?: return
        val record = MilestoneRecord(
            sessionId = sid,
            milestone = milestone,
            metadata = metadata,
        )
        milestones.add(record)
    }

    fun getMilestones(): List<MilestoneRecord> = milestones.toList()

    fun getTimeline(): List<Pair<LaunchMilestone, Long>> {
        return milestones.map { it.milestone to it.timestamp }
    }

    fun isMonotonic(): Boolean {
        var lastTime = 0L
        for (record in milestones) {
            if (record.timestamp < lastTime) return false
            lastTime = record.timestamp
        }
        return true
    }

    fun clear() {
        milestones.clear()
        sessionId = null
    }
}

/**
 * Global singleton emitter for launch milestones.
 *
 * Provides a convenient facade for recording milestones across the
 * application without requiring dependency injection of MilestoneRecorder.
 *
 * @see LaunchMilestone
 * @see MilestoneRecorder
 */
object MilestoneEmitter {
    private val recorder = MilestoneRecorder()

    fun startSession(sessionId: String) {
        recorder.startSession(sessionId)
    }

    fun record(milestone: LaunchMilestone, metadata: Map<String, String> = emptyMap()) {
        recorder.record(milestone, metadata)
    }

    fun getRecorder(): MilestoneRecorder = recorder
}
