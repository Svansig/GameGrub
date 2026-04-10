package app.gamegrub.ui.screen.xserver

import com.winlator.xenvironment.EnvironmentStopSummary

data class XServerTeardownPhase(
    val name: String,
    val durationMs: Long,
    val success: Boolean,
    val errorMessage: String? = null,
)

data class XServerTeardownReport(
    val appId: String,
    val totalDurationMs: Long,
    val phases: List<XServerTeardownPhase>,
    val environmentStopSummary: EnvironmentStopSummary? = null,
    val postExitHealthReport: XServerPostExitHealthReport? = null,
) {
    fun hasFailures(): Boolean {
        val phaseFailure = phases.any { !it.success }
        val environmentFailure = (environmentStopSummary?.failedCount ?: 0) > 0
        val healthFailure = postExitHealthReport?.hasLeftovers() == true
        return phaseFailure || environmentFailure || healthFailure
    }

    fun toLogLine(): String {
        val phaseSummary = phases.joinToString(separator = ",") {
            if (it.success) {
                "${it.name}:${it.durationMs}ms"
            } else {
                "${it.name}:FAILED(${it.durationMs}ms)"
            }
        }
        val envSummary = environmentStopSummary?.let {
            "envStop=${it.totalDurationMs}ms,envFailed=${it.failedCount}"
        } ?: "envStop=NA"
        val healthSummary = postExitHealthReport?.let {
            "leftoverWine=${it.runningWineProcessCount},leftoverThreads=${it.lingeringRuntimeThreadCount}"
        } ?: "leftover=NA"
        return "[Teardown][appId=$appId][total=${totalDurationMs}ms][$envSummary][$healthSummary][phases=$phaseSummary]"
    }
}

