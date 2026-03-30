package app.gamegrub.ui.screen.xserver

import app.gamegrub.GameGrubApp
import app.gamegrub.events.AndroidEvent
import app.gamegrub.events.SteamEvent
import com.winlator.core.Callback
import com.winlator.core.ProcessHelper

/**
 * Centralizes event bus and debug callback lifecycle wiring for XServerScreen.
 */
internal object XServerLifecycleEventBindings {
    fun register(
        onActivityDestroyed: (AndroidEvent.ActivityDestroyed) -> Unit,
        onKeyEvent: (AndroidEvent.KeyEvent) -> Boolean,
        onMotionEvent: (AndroidEvent.MotionEvent) -> Boolean,
        onGuestProgramTerminated: (AndroidEvent.GuestProgramTerminated) -> Unit,
        onForceCloseApp: (SteamEvent.ForceCloseApp) -> Unit,
        debugCallback: Callback<String>,
        addDebugCallback: (Callback<String>) -> Unit = ProcessHelper::addDebugCallback,
        removeDebugCallback: (Callback<String>) -> Unit = ProcessHelper::removeDebugCallback,
    ): () -> Unit {
        GameGrubApp.events.on<AndroidEvent.ActivityDestroyed, Unit>(onActivityDestroyed)
        GameGrubApp.events.on<AndroidEvent.KeyEvent, Boolean>(onKeyEvent)
        GameGrubApp.events.on<AndroidEvent.MotionEvent, Boolean>(onMotionEvent)
        GameGrubApp.events.on<AndroidEvent.GuestProgramTerminated, Unit>(onGuestProgramTerminated)
        GameGrubApp.events.on<SteamEvent.ForceCloseApp, Unit>(onForceCloseApp)
        addDebugCallback(debugCallback)

        return {
            GameGrubApp.events.off<AndroidEvent.ActivityDestroyed, Unit>(onActivityDestroyed)
            GameGrubApp.events.off<AndroidEvent.KeyEvent, Boolean>(onKeyEvent)
            GameGrubApp.events.off<AndroidEvent.MotionEvent, Boolean>(onMotionEvent)
            GameGrubApp.events.off<AndroidEvent.GuestProgramTerminated, Unit>(onGuestProgramTerminated)
            GameGrubApp.events.off<SteamEvent.ForceCloseApp, Unit>(onForceCloseApp)
            removeDebugCallback(debugCallback)
        }
    }
}
