package app.gamegrub.ui.screen.xserver

import app.gamegrub.GameGrubApp
import app.gamegrub.ui.runtime.XServerRuntime
import com.winlator.container.Container
import com.winlator.widget.FrameRating
import com.winlator.widget.XServerView
import com.winlator.winhandler.OnGetProcessInfoListener
import com.winlator.winhandler.ProcessInfo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Watches WinHandler process snapshots after a game window is unmapped and
 * triggers coordinated exit once only essential processes remain.
 */
internal object XServerExitWatchHelper {
    fun start(
        existingJob: Job?,
        xServerView: XServerView?,
        window: com.winlator.xserver.Window,
        container: Container,
        frameRating: FrameRating?,
        appInfo: app.gamegrub.data.SteamApp?,
        appId: String,
        onExit: (onComplete: (() -> Unit)?) -> Unit,
        navigateBack: () -> Unit,
        processTimeoutMs: Long,
        pollIntervalMs: Long,
        responseTimeoutMs: Long,
    ): Job? {
        val winHandler = xServerView?.getxServer()?.winHandler ?: return existingJob
        if (existingJob?.isActive == true) {
            return existingJob
        }

        val targetExecutable = XServerProcessMatcher.extractExecutableBasename(container.executablePath)
        if (!XServerProcessMatcher.windowMatchesExecutable(window, targetExecutable)) {
            return existingJob
        }

        return CoroutineScope(Dispatchers.IO).launch {
            val allowlist = XServerProcessMatcher.buildEssentialProcessAllowlist()
            val previousListener = winHandler.onGetProcessInfoListener
            val lock = Any()
            var pendingSnapshot: CompletableDeferred<List<ProcessInfo>?>? = null
            var currentList = mutableListOf<ProcessInfo>()
            var expectedCount = 0

            val listener = OnGetProcessInfoListener { index, count, processInfo ->
                previousListener?.onGetProcessInfo(index, count, processInfo)
                synchronized(lock) {
                    val deferred = pendingSnapshot ?: return@synchronized
                    if (count == 0 && processInfo == null) {
                        if (!deferred.isCompleted) {
                            deferred.complete(null)
                        }
                        return@synchronized
                    }
                    if (index == 0) {
                        currentList = mutableListOf()
                        expectedCount = count
                    }
                    if (processInfo != null) {
                        currentList.add(processInfo)
                    }
                    if (currentList.size >= expectedCount && !deferred.isCompleted) {
                        deferred.complete(currentList.toList())
                    }
                }
            }

            winHandler.onGetProcessInfoListener = listener
            try {
                val startTime = System.currentTimeMillis()
                while (System.currentTimeMillis() - startTime < processTimeoutMs) {
                    val deferred = CompletableDeferred<List<ProcessInfo>?>()
                    synchronized(lock) {
                        pendingSnapshot = deferred
                    }
                    winHandler.listProcesses()
                    val snapshot = withTimeoutOrNull(responseTimeoutMs) {
                        deferred.await()
                    }
                    if (snapshot != null) {
                        val hasNonEssential = snapshot.any {
                            !allowlist.contains(XServerProcessMatcher.normalizeProcessName(it.name))
                        }
                        if (!hasNonEssential) {
                            withContext(Dispatchers.Main) {
                                XServerExitCoordinator.requestExit(
                                    winHandler = winHandler,
                                    environment = XServerRuntime.get().xEnvironment,
                                    frameRating = frameRating,
                                    appInfo = appInfo,
                                    container = container,
                                    appId = appId,
                                    onExit = onExit,
                                    navigateBack = navigateBack,
                                )
                            }
                            break
                        }
                    }
                    delay(pollIntervalMs)
                }
            } finally {
                winHandler.onGetProcessInfoListener = previousListener
                synchronized(lock) {
                    pendingSnapshot = null
                }
            }
        }
    }
}
