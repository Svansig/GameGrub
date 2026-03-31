package app.gamegrub.ui.screen.xserver

import app.gamegrub.events.AndroidEvent
import app.gamegrub.events.SteamEvent
import timber.log.Timber

/**
 * Creates event handlers that map different termination signals to one exit callback.
 */
internal object XServerExitTriggerEventCoordinator {
    enum class ExitTrigger {
        GUEST_PROGRAM_TERMINATED,
        FORCE_CLOSE_APP,
    }

    data class Handlers(
        val onGuestProgramTerminated: (AndroidEvent.GuestProgramTerminated) -> Unit,
        val onForceCloseApp: (SteamEvent.ForceCloseApp) -> Unit,
    )

    fun createHandlers(
        onRequestExit: (ExitTrigger) -> Unit,
        logInfo: (String) -> Unit = { message -> Timber.i(message) },
    ): Handlers {
        return Handlers(
            onGuestProgramTerminated = {
                logInfo("onGuestProgramTerminated")
                onRequestExit(ExitTrigger.GUEST_PROGRAM_TERMINATED)
            },
            onForceCloseApp = {
                logInfo("onForceCloseApp")
                onRequestExit(ExitTrigger.FORCE_CLOSE_APP)
            },
        )
    }
}
