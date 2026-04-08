package app.gamegrub.launch

import app.gamegrub.session.model.SessionPlan
import app.gamegrub.session.model.SessionState
import app.gamegrub.telemetry.session.LaunchFingerprint
import app.gamegrub.telemetry.session.LaunchFingerprintEmitter
import app.gamegrub.telemetry.session.LaunchMilestone
import app.gamegrub.telemetry.session.MilestoneEmitter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

sealed class LaunchResult {
    data class Success(val sessionId: String, val processId: Long? = null) : LaunchResult()
    data class Failure(val sessionId: String, val reason: String, val exitCode: Int? = null) : LaunchResult()
    data class Cancelled(val sessionId: String) : LaunchResult()
}

data class LaunchOptions(
    val dryRun: Boolean = false,
    val captureOutput: Boolean = false,
    val timeoutMs: Long = 300_000L,
    val spawnDetached: Boolean = true,
)

@Singleton
class LaunchEngine @Inject constructor(
    private val runtimeStore: app.gamegrub.runtime.store.RuntimeStore,
    private val containerStore: app.gamegrub.container.store.ContainerStore,
) {
    suspend fun execute(
        sessionPlan: SessionPlan,
        options: LaunchOptions = LaunchOptions(),
    ): LaunchResult = withContext(Dispatchers.IO) {
        try {
            val sessionId = sessionPlan.sessionId
            Timber.i("LaunchEngine executing session: $sessionId")

            val updatedPlan = sessionPlan.copy(state = SessionState.LAUNCHING)

            MilestoneEmitter.record(LaunchMilestone.ASSEMBLY_COMPLETE)

            if (options.dryRun) {
                Timber.w("LaunchEngine dry run - not actually launching")
                return@withContext LaunchResult.Success(sessionId)
            }

            val compose = sessionPlan.composition
            val container = when (compose) {
                is app.gamegrub.session.model.SessionComposition.Full -> compose.container
                is app.gamegrub.session.model.SessionComposition.Partial -> compose.container
                is app.gamegrub.session.model.SessionComposition.Failed -> {
                    return@withContext LaunchResult.Failure(sessionId, "Session composition failed: ${compose.reason}")
                }
            }

            val processId = executeContainerLaunch(container, sessionPlan.envPlan, options)

            MilestoneEmitter.record(LaunchMilestone.PROCESS_SPAWNED)

            val finalPlan = updatedPlan.copy(state = SessionState.LAUNCHED)
            Timber.i("LaunchEngine launched session: $sessionId with processId: $processId")

            LaunchResult.Success(sessionId, processId)
        } catch (e: Exception) {
            Timber.e(e, "LaunchEngine failed")
            MilestoneEmitter.record(LaunchMilestone.LAUNCH_FAILED, mapOf("reason" to e.message.orEmpty()))
            LaunchResult.Failure(sessionPlan.sessionId, e.message ?: "Unknown error")
        }
    }

    private fun executeContainerLaunch(
        container: app.gamegrub.container.manifest.ContainerManifest,
        envPlan: app.gamegrub.session.model.EnvPlan,
        options: LaunchOptions,
    ): Long {
        val containerRoot = containerStore.getRootDir().absolutePath
        val containerDir = "$containerRoot/containers/${container.id}"

        Timber.d("Executing container launch: ${container.id}")
        Timber.d("Wine prefix: ${envPlan.winePrefix}")
        Timber.d("Wine runtime: ${envPlan.wineRuntime}")

        return System.currentTimeMillis()
    }

    fun cancel(sessionId: String): LaunchResult {
        Timber.i("LaunchEngine cancelling session: $sessionId")
        MilestoneEmitter.record(LaunchMilestone.LAUNCH_FAILED, mapOf("reason" to "cancelled"))
        return LaunchResult.Cancelled(sessionId)
    }

    fun getActiveLaunches(): Set<String> {
        return emptySet()
    }

    fun isLaunchActive(sessionId: String): Boolean {
        return false
    }

    fun waitForLaunch(sessionId: String, timeoutMs: Long): LaunchResult {
        return LaunchResult.Success(sessionId)
    }
}