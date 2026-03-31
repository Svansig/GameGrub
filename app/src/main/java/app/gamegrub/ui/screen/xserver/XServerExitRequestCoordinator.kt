package app.gamegrub.ui.screen.xserver

import app.gamegrub.data.SteamApp
import com.winlator.container.Container
import com.winlator.widget.FrameRating
import com.winlator.winhandler.WinHandler
import com.winlator.xenvironment.XEnvironment

/**
 * Encapsulates the common exit request payload used across XServerScreen callbacks.
 */
internal class XServerExitRequestCoordinator(
    private val winHandlerProvider: () -> WinHandler?,
    private val environmentProvider: () -> XEnvironment?,
    private val frameRatingProvider: () -> FrameRating?,
    private val appInfoProvider: () -> SteamApp?,
    private val container: Container,
    private val appId: String,
    private val onExit: (onComplete: (() -> Unit)?) -> Unit,
    private val navigateBack: () -> Unit,
) {
    fun requestExit() {
        val winHandler = requireNotNull(winHandlerProvider())
        XServerExitCoordinator.requestExit(
            winHandler = winHandler,
            environment = environmentProvider(),
            frameRating = frameRatingProvider(),
            appInfo = appInfoProvider(),
            container = container,
            appId = appId,
            onExit = onExit,
            navigateBack = navigateBack,
        )
    }
}
