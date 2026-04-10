package app.gamegrub.ui.screen.xserver

import app.gamegrub.LaunchRequestManager
import app.gamegrub.data.SteamApp
import app.gamegrub.ui.runtime.XServerRuntime
import app.gamegrub.utils.container.ContainerUtils
import com.posthog.PostHog
import com.winlator.container.Container
import com.winlator.widget.FrameRating
import com.winlator.winhandler.WinHandler
import com.winlator.xenvironment.EnvironmentStopSummary
import com.winlator.xenvironment.XEnvironment
import java.util.concurrent.atomic.AtomicBoolean
import timber.log.Timber

internal object XServerExitCoordinator {
    private val isExiting = AtomicBoolean(false)

    fun isExitInProgress(): Boolean {
        return isExiting.get()
    }

    fun resetExitGuard() {
        isExiting.set(false)
    }

    fun requestExit(
        winHandler: WinHandler?,
        environment: XEnvironment?,
        frameRating: FrameRating?,
        appInfo: SteamApp?,
        container: Container,
        appId: String,
        onExit: (onComplete: (() -> Unit)?) -> Unit,
        navigateBack: () -> Unit,
    ) {
        Timber.i("Exit called")

        if (!isExiting.compareAndSet(false, true)) {
            Timber.i("Exit already in progress, ignoring duplicate request")
            return
        }

        PostHog.capture(
            event = "game_exited",
            properties = mapOf(
                "game_name" to ContainerUtils.resolveGameName(appId),
                "game_store" to ContainerUtils.extractGameSourceFromContainerId(appId).name,
                "session_length" to (frameRating?.sessionLengthSec ?: 0),
                "avg_fps" to (frameRating?.avgFPS ?: 0.0),
                "container_config" to container.containerJson,
            ),
        )

        frameRating?.let { rating ->
            container.putSessionMetadata("avg_fps", rating.avgFPS)
            container.putSessionMetadata("session_length_sec", rating.sessionLengthSec.toInt())
            container.saveData()
        }

        val teardownStartNs = System.nanoTime()
        val teardownPhases: MutableList<XServerTeardownPhase> = mutableListOf()
        var environmentStopSummary: EnvironmentStopSummary? = null
        var postExitHealthReport: XServerPostExitHealthReport? = null

        fun runPhase(name: String, block: () -> Unit) {
            val phaseStartNs = System.nanoTime()
            runCatching(block)
                .onSuccess {
                    teardownPhases.add(
                        XServerTeardownPhase(
                            name = name,
                            durationMs = (System.nanoTime() - phaseStartNs) / 1_000_000L,
                            success = true,
                        ),
                    )
                }
                .onFailure { error ->
                    teardownPhases.add(
                        XServerTeardownPhase(
                            name = name,
                            durationMs = (System.nanoTime() - phaseStartNs) / 1_000_000L,
                            success = false,
                            errorMessage = error.message,
                        ),
                    )
                    Timber.e(error, "[Teardown] Phase failed: $name")
                }
        }

        runPhase("StopSessionWatchers") {
            XServerRuntime.get().stopAndClearAchievementWatcher()
            app.gamegrub.service.steam.SteamService.clearCachedAchievements()
        }

        runPhase("StopInputAndWinHandler") {
            XServerRuntime.get().touchpadView?.releasePointerCapture()
            winHandler?.stop()
        }

        runPhase("StopEnvironmentComponents") {
            environmentStopSummary = environment?.stopEnvironmentComponentsWithSummary()
        }

        runPhase("ClearRuntimeReferences") {
            app.gamegrub.service.steam.SteamService.keepAlive = false
            XServerRuntime.get().clearActiveSuspendState()
            XServerRuntime.get().clearXEnvironment()
            XServerRuntime.get().clearInputControlsView()
            XServerRuntime.get().clearInputControlsManager()
            XServerRuntime.get().clearTouchpadView()
        }

        runPhase("WriteSessionSummary") {
            frameRating?.writeSessionSummary()
        }

        runPhase("PostExitHealthChecks") {
            postExitHealthReport = XServerPostExitHealthChecker.check()
        }

        val teardownReport = XServerTeardownReport(
            appId = appId,
            totalDurationMs = (System.nanoTime() - teardownStartNs) / 1_000_000L,
            phases = teardownPhases,
            environmentStopSummary = environmentStopSummary,
            postExitHealthReport = postExitHealthReport,
        )
        if (teardownReport.hasFailures()) {
            Timber.w(teardownReport.toLogLine())
        } else {
            Timber.i(teardownReport.toLogLine())
        }

        if (LaunchRequestManager.wasLaunchedViaExternalIntent) {
            Timber.i("[IntentLaunch]: Waiting for exit handling before returning to external launcher")
            onExit(navigateBack)
        } else {
            onExit(null)
            navigateBack()
        }
    }
}
