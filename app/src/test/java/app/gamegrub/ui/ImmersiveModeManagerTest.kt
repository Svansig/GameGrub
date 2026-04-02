package app.gamegrub.ui

import android.app.Activity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for [ImmersiveModeManager].
 *
 * These tests focus on intent-level manager behavior (requested visibility state) and
 * lifecycle-safe reapplication entry points.
 */
@RunWith(RobolectricTestRunner::class)
class ImmersiveModeManagerTest {

    /**
     * Verify manager defaults to hidden system UI intent.
     */
    @Test
    fun isSystemUIVisible_defaultsToFalse() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val manager = ImmersiveModeManager(activity.window)

        assertFalse(manager.isSystemUIVisible())
    }

    /**
     * Verify explicit visibility requests are persisted in manager state.
     */
    @Test
    fun setSystemUIVisibility_updatesRequestedState() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val manager = ImmersiveModeManager(activity.window)

        manager.setSystemUIVisibility(true)
        assertTrue(manager.isSystemUIVisible())

        manager.setSystemUIVisibility(false)
        assertFalse(manager.isSystemUIVisible())
    }

    /**
     * Verify focus callbacks do not mutate requested state and remain safe in repeated calls.
     */
    @Test
    fun onWindowFocusChanged_reappliesWithoutChangingRequestedState() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val manager = ImmersiveModeManager(activity.window)

        manager.setSystemUIVisibility(false)
        manager.onWindowFocusChanged(false)
        manager.onWindowFocusChanged(true)

        assertFalse(manager.isSystemUIVisible())
    }
}

