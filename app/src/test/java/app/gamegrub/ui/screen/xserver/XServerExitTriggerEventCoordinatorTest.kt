package app.gamegrub.ui.screen.xserver

import app.gamegrub.events.AndroidEvent
import app.gamegrub.events.SteamEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class XServerExitTriggerEventCoordinatorTest {

    @Test
    fun createHandlers_guestProgramTerminated_logsAndRequestsGuestExit() {
        val logs = mutableListOf<String>()
        val triggers = mutableListOf<XServerExitTriggerEventCoordinator.ExitTrigger>()

        val handlers = XServerExitTriggerEventCoordinator.createHandlers(
            onRequestExit = { trigger -> triggers += trigger },
            logInfo = { message -> logs += message },
        )

        handlers.onGuestProgramTerminated(AndroidEvent.GuestProgramTerminated)

        assertEquals(listOf("onGuestProgramTerminated"), logs)
        assertEquals(
            listOf(XServerExitTriggerEventCoordinator.ExitTrigger.GUEST_PROGRAM_TERMINATED),
            triggers,
        )
    }

    @Test
    fun createHandlers_forceCloseApp_logsAndRequestsForceCloseExit() {
        val logs = mutableListOf<String>()
        val triggers = mutableListOf<XServerExitTriggerEventCoordinator.ExitTrigger>()

        val handlers = XServerExitTriggerEventCoordinator.createHandlers(
            onRequestExit = { trigger -> triggers += trigger },
            logInfo = { message -> logs += message },
        )

        handlers.onForceCloseApp(SteamEvent.ForceCloseApp)

        assertEquals(listOf("onForceCloseApp"), logs)
        assertEquals(
            listOf(XServerExitTriggerEventCoordinator.ExitTrigger.FORCE_CLOSE_APP),
            triggers,
        )
    }
}

