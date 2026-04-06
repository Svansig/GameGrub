package app.gamegrub.ui.screen.xserver

import app.gamegrub.GameGrubApp
import app.gamegrub.events.AndroidEvent
import app.gamegrub.events.SteamEvent
import com.winlator.core.Callback
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test

class XServerLifecycleEventBindingsTest {

    @Test
    fun register_registersAllHandlersAndDisposeUnregistersThem() {
        GameGrubApp.events.clearAllListeners()

        var debugAddCalls = 0
        var debugRemoveCalls = 0
        var addedCallback: Callback<String>? = null
        var removedCallback: Callback<String>? = null

        val debugCallback = Callback<String> { }
        val dispose = XServerLifecycleEventBindings.register(
            onActivityDestroyed = { },
            onKeyEvent = { true },
            onMotionEvent = { false },
            onGuestProgramTerminated = { },
            onForceCloseApp = { },
            debugCallback = debugCallback,
            addDebugCallback = { callback ->
                debugAddCalls++
                addedCallback = callback
            },
            removeDebugCallback = { callback ->
                debugRemoveCalls++
                removedCallback = callback
            },
        )

        assertNotNull(GameGrubApp.events.listeners[AndroidEvent.ActivityDestroyed::class])
        assertNotNull(GameGrubApp.events.listeners[AndroidEvent.KeyEvent::class])
        assertNotNull(GameGrubApp.events.listeners[AndroidEvent.MotionEvent::class])
        assertNotNull(GameGrubApp.events.listeners[AndroidEvent.GuestProgramTerminated::class])
        assertNotNull(GameGrubApp.events.listeners[SteamEvent.ForceCloseApp::class])
        assertEquals(1, debugAddCalls)
        assertSame(debugCallback, addedCallback)

        dispose()

        assertEquals(0, GameGrubApp.events.listeners[AndroidEvent.ActivityDestroyed::class]?.size ?: 0)
        assertEquals(0, GameGrubApp.events.listeners[AndroidEvent.KeyEvent::class]?.size ?: 0)
        assertEquals(0, GameGrubApp.events.listeners[AndroidEvent.MotionEvent::class]?.size ?: 0)
        assertEquals(0, GameGrubApp.events.listeners[AndroidEvent.GuestProgramTerminated::class]?.size ?: 0)
        assertEquals(0, GameGrubApp.events.listeners[SteamEvent.ForceCloseApp::class]?.size ?: 0)
        assertEquals(1, debugRemoveCalls)
        assertSame(debugCallback, removedCallback)
    }
}
