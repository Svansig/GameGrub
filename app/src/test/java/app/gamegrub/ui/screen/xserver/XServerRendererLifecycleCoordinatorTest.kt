package app.gamegrub.ui.screen.xserver

import androidx.lifecycle.Lifecycle
import org.junit.Assert.assertEquals
import org.junit.Test

class XServerRendererLifecycleCoordinatorTest {

    @Test
    fun resolveRendererSyncAction_returnsNone_whenViewIsNotAttached() {
        val action = XServerRendererLifecycleCoordinator.resolveRendererSyncAction(
            isAttachedToWindow = false,
            lifecycleState = Lifecycle.State.RESUMED,
        )

        assertEquals(XServerRendererLifecycleCoordinator.RendererSyncAction.NONE, action)
    }

    @Test
    fun resolveRendererSyncAction_returnsNone_whenLifecycleDestroyed() {
        val action = XServerRendererLifecycleCoordinator.resolveRendererSyncAction(
            isAttachedToWindow = true,
            lifecycleState = Lifecycle.State.DESTROYED,
        )

        assertEquals(XServerRendererLifecycleCoordinator.RendererSyncAction.NONE, action)
    }

    @Test
    fun resolveRendererSyncAction_returnsResume_whenLifecycleResumedOrHigher() {
        val action = XServerRendererLifecycleCoordinator.resolveRendererSyncAction(
            isAttachedToWindow = true,
            lifecycleState = Lifecycle.State.RESUMED,
        )

        assertEquals(XServerRendererLifecycleCoordinator.RendererSyncAction.RESUME, action)
    }

    @Test
    fun resolveRendererSyncAction_returnsPause_whenAttachedButNotResumed() {
        val action = XServerRendererLifecycleCoordinator.resolveRendererSyncAction(
            isAttachedToWindow = true,
            lifecycleState = Lifecycle.State.STARTED,
        )

        assertEquals(XServerRendererLifecycleCoordinator.RendererSyncAction.PAUSE, action)
    }
}

