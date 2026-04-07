package app.gamegrub.ui.screen.xserver

import android.app.Activity
import android.content.Context
import android.view.View
import androidx.compose.runtime.MutableState
import app.gamegrub.data.SteamApp
import app.gamegrub.ui.data.XServerState
import com.winlator.container.Container
import com.winlator.core.Win32AppWorkarounds
import com.winlator.widget.FrameRating
import com.winlator.widget.XServerView
import com.winlator.xserver.Property
import com.winlator.xserver.Window
import com.winlator.xserver.WindowManager
import kotlinx.coroutines.Job
import timber.log.Timber

private const val NO_FRAME_RATING_WINDOW_ID = -1

/**
 * Coordinates XServer window lifecycle callbacks that are still owned by the screen:
 * - first application window detection
 * - frame rating visibility/update tracking
 * - mapped/unmapped logging and callbacks
 * - exit-watch startup after game windows unmap
 */
internal class XServerWindowLifecycleCoordinator(
    context: Context,
    private val container: Container,
    private val appId: String,
    private val appInfo: SteamApp?,
    private val xServerState: MutableState<XServerState>,
    private val frameRatingProvider: () -> FrameRating?,
    private val currentExitWatchJobProvider: () -> Job?,
    private val updateExitWatchJob: (Job?) -> Unit,
    private val onFirstApplicationWindowStarted: () -> Unit,
    private val win32AppWorkaroundsProvider: () -> Win32AppWorkarounds?,
    private val onWindowMapped: (Window) -> Unit,
    private val onWindowUnmapped: (Window) -> Unit,
    private val onExit: (onComplete: (() -> Unit)?) -> Unit,
    private val navigateBack: () -> Unit,
    private val processTimeoutMs: Long,
    private val pollIntervalMs: Long,
    private val responseTimeoutMs: Long,
) {
    private val activity: Activity? = context as? Activity

    private var attachedXServerView: XServerView? = null
    private var attachedWindowManager: WindowManager? = null
    private var windowModificationListener: WindowManager.OnWindowModificationListener? = null
    private var frameRatingWindowId: Int = NO_FRAME_RATING_WINDOW_ID

    fun attach(xServerView: XServerView) {
        detach()

        val listener = object : WindowManager.OnWindowModificationListener {
            override fun onUpdateWindowContent(window: Window) {
                if (!xServerState.value.winStarted && window.isApplicationWindow()) {
                    onFirstApplicationWindowStarted()
                    xServerState.value.winStarted = true
                }
                if (window.id == frameRatingWindowId) {
                    activity?.runOnUiThread {
                        frameRatingProvider()?.update()
                    }
                }
            }

            override fun onModifyWindowProperty(window: Window, property: Property) {
                updateFrameRatingVisibility(window, property)
            }

            override fun onMapWindow(window: Window) {
                logWindowEvent("onMapWindow", window)
                win32AppWorkaroundsProvider()?.applyWindowWorkarounds(window)
                onWindowMapped(window)
            }

            override fun onUnmapWindow(window: Window) {
                logWindowEvent("onUnmapWindow", window)
                updateFrameRatingVisibility(window, property = null)
                startExitWatchForUnmappedGameWindow(window)
                onWindowUnmapped(window)
            }
        }

        xServerView.getxServer().windowManager.addOnWindowModificationListener(listener)
        attachedXServerView = xServerView
        attachedWindowManager = xServerView.getxServer().windowManager
        windowModificationListener = listener
    }

    fun detach() {
        attachedWindowManager?.let { windowManager ->
            windowModificationListener?.let(windowManager::removeOnWindowModificationListener)
        }
        attachedXServerView = null
        attachedWindowManager = null
        windowModificationListener = null
        frameRatingWindowId = NO_FRAME_RATING_WINDOW_ID
    }

    fun cancelActiveExitWatch() {
        currentExitWatchJobProvider()?.cancel()
        updateExitWatchJob(null)
    }

    private fun startExitWatchForUnmappedGameWindow(window: Window) {
        updateExitWatchJob(
            XServerExitWatchHelper.start(
                existingJob = currentExitWatchJobProvider(),
                xServerView = attachedXServerView,
                window = window,
                container = container,
                frameRating = frameRatingProvider(),
                appInfo = appInfo,
                appId = appId,
                onExit = onExit,
                navigateBack = navigateBack,
                processTimeoutMs = processTimeoutMs,
                pollIntervalMs = pollIntervalMs,
                responseTimeoutMs = responseTimeoutMs,
            ),
        )
    }

    private fun updateFrameRatingVisibility(window: Window, property: Property?) {
        val frameRating = frameRatingProvider() ?: return
        if (property != null) {
            val propertyName = property.nameAsString()
            val shouldShowFrameRating = frameRatingWindowId == NO_FRAME_RATING_WINDOW_ID &&
                    (
                            propertyName.contains("_UTIL_LAYER") ||
                                    propertyName.contains("_MESA_DRV") ||
                                    (
                                            container.containerVariant.equals(Container.GLIBC) &&
                                                    propertyName.contains("_NET_WM_SURFACE")
                                            )
                            )
            if (shouldShowFrameRating) {
                frameRatingWindowId = window.id
                activity?.runOnUiThread {
                    frameRating.visibility = View.VISIBLE
                }
                frameRating.update()
            }
        } else if (frameRatingWindowId != NO_FRAME_RATING_WINDOW_ID) {
            frameRatingWindowId = NO_FRAME_RATING_WINDOW_ID
            activity?.runOnUiThread {
                frameRating.visibility = View.GONE
            }
        }
    }

    private fun logWindowEvent(eventName: String, window: Window) {
        Timber.i(
            "%s:\n\twindowName: %s\n\twindowClassName: %s\n\tprocessId: %s\n\thasParent: %s\n\tchildrenSize: %s",
            eventName,
            window.name,
            window.className,
            window.processId,
            window.parent != null,
            window.children.size,
        )
    }
}
