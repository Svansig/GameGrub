package app.gamegrub.telemetry.session

import kotlinx.serialization.Serializable

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

@Serializable
data class MilestoneRecord(
    val sessionId: String,
    val milestone: LaunchMilestone,
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: Map<String, String> = emptyMap(),
)

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