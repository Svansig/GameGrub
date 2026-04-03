package app.gamegrub.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.OrientationEventListener
import app.gamegrub.ui.enums.Orientation
import app.gamegrub.ui.orientation.OrientationPolicy
import java.util.EnumSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OrientationManagerTest {

    @Test
    fun setOrientationPolicy_withUnspecifiedOverride_setsActivityToUnspecified() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val manager = OrientationManager(activity)

        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        manager.setOrientationPolicy(
            OrientationPolicy.unrestricted(
                EnumSet.of(Orientation.LANDSCAPE, Orientation.REVERSE_LANDSCAPE),
            ),
        )

        assertEquals(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, activity.requestedOrientation)
    }

    @Test
    fun applyOrientationSensorValue_unknownOrientation_keepsCurrentRequestedOrientation() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val manager = OrientationManager(activity)

        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
        manager.setOrientationPolicy(OrientationPolicy.default(EnumSet.of(Orientation.LANDSCAPE)))

        manager.applyOrientationSensorValue(OrientationEventListener.ORIENTATION_UNKNOWN)

        assertEquals(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE, activity.requestedOrientation)
    }

    @Test
    fun applyOrientationSensorValue_selectsNearestAllowedOrientation() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val manager = OrientationManager(activity)
        manager.setOrientationPolicy(
            OrientationPolicy.default(
                EnumSet.of(Orientation.LANDSCAPE, Orientation.REVERSE_LANDSCAPE),
            ),
        )

        // 270 input maps to 90 adjusted, which is landscape.
        manager.applyOrientationSensorValue(270)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, activity.requestedOrientation)

        // 90 input maps to 270 adjusted, which is reverse landscape.
        manager.applyOrientationSensorValue(90)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE, activity.requestedOrientation)
    }

    @Test
    fun startStopOrientator_isIdempotentAcrossLifecycleTransitions() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val manager = OrientationManager(activity)

        manager.startOrientator()
        manager.startOrientator()

        val listenerField = OrientationManager::class.java.getDeclaredField("orientationSensorListener")
        listenerField.isAccessible = true
        assertNotNull(listenerField.get(manager))

        manager.stopOrientator()
        manager.stopOrientator()

        assertNull(listenerField.get(manager))

        manager.startOrientator()
        assertNotNull(listenerField.get(manager))
    }
}


