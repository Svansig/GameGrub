package app.gamegrub.ui.screen.xserver

import app.gamegrub.events.AndroidEvent
import app.gamegrub.events.SteamEvent
import timber.log.Timber

/**
 * Unifies lifecycle/event-driven exit handlers into one request-exit callback.
 */
internal object XServerLifecycleExitEventCoordinator {
    data class Handlers(
        val onActivityDestroyed: (AndroidEvent.ActivityDestroyed) -> Unit,
        val onGuestProgramTerminated: (AndroidEvent.GuestProgramTerminated) -> Unit,
        val onForceCloseApp: (SteamEvent.ForceCloseApp) -> Unit,
    )

    fun createHandlers(
        onRequestExit: () -> Unit,
        logInfo: (String) -> Unit = { message -> Timber.i(message) },
    ): Handlers {
        val exitTriggerHandlers = XServerExitTriggerEventCoordinator.createHandlers(
            onRequestExit = { _ -> onRequestExit() },
            logInfo = logInfo,
        )

        return Handlers(
            onActivityDestroyed = {
                logInfo("onActivityDestroyed")
                onRequestExit()
            },
            onGuestProgramTerminated = exitTriggerHandlers.onGuestProgramTerminated,
            onForceCloseApp = exitTriggerHandlers.onForceCloseApp,
        )
    }
}
