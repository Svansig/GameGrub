package app.gamegrub.ui.screen.xserver

import app.gamegrub.GameGrubApp
import app.gamegrub.LaunchRequestManager
import app.gamegrub.data.SteamApp
import app.gamegrub.ui.runtime.XServerRuntime
import app.gamegrub.utils.container.ContainerUtils
import com.posthog.PostHog
import com.winlator.container.Container
import com.winlator.widget.FrameRating
import com.winlator.winhandler.WinHandler
import com.winlator.xenvironment.XEnvironment
import java.util.concurrent.atomic.AtomicBoolean
import timber.log.Timber

internal object XServerExitCoordinator {
    private val isExiting = AtomicBoolean(false)

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

        XServerRuntime.get().stopAndClearAchievementWatcher()
        app.gamegrub.service.steam.SteamService.clearCachedAchievements()

        XServerRuntime.get().touchpadView?.releasePointerCapture()
        winHandler?.stop()
        environment?.stopEnvironmentComponents()
        app.gamegrub.service.steam.SteamService.keepAlive = false
        XServerRuntime.get().clearActiveSuspendState()
        XServerRuntime.get().clearXEnvironment()
        XServerRuntime.get().clearInputControlsView()
        XServerRuntime.get().clearInputControlsManager()
        XServerRuntime.get().clearTouchpadView()
        frameRating?.writeSessionSummary()

        if (LaunchRequestManager.wasLaunchedViaExternalIntent) {
            Timber.i("[IntentLaunch]: Waiting for exit handling before returning to external launcher")
            onExit(navigateBack)
        } else {
            onExit(null)
            navigateBack()
        }
    }
}
