package app.gamegrub.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.OrientationEventListener
import app.gamegrub.ui.enums.Orientation
import app.gamegrub.ui.orientation.OrientationPolicy
import java.util.EnumSet
import kotlin.math.abs
import timber.log.Timber

/**
 * Manages device orientation changes and applies orientation constraints.
 * Uses orientation sensor to detect device rotation and applies user-defined
 * orientation preferences.
 */
class OrientationManager(private val activity: Activity) {

    private var orientationSensorListener: OrientationEventListener? = null

    private var currentOrientationChangeValue: Int = ORIENTATION_UNKNOWN
    private var orientationPolicy: OrientationPolicy = OrientationPolicy.default(EnumSet.of(Orientation.UNSPECIFIED))

    /**
     * Start listening to orientation sensor.
     */
    fun startOrientator() {
        if (orientationSensorListener != null) {
            return
        }

        orientationSensorListener = object : OrientationEventListener(activity) {
            override fun onOrientationChanged(orientation: Int) {
                applyOrientationSensorValue(orientation)
            }
        }

        orientationSensorListener?.takeIf { it.canDetectOrientation() }?.enable()
        applyOrientationPolicy()
    }

    /**
     * Stop listening to orientation sensor.
     */
    fun stopOrientator() {
        val listener = orientationSensorListener ?: return
        listener.disable()
        orientationSensorListener = null
    }

    /**
     * Update available orientations and reapply orientation constraints.
     */
    fun setOrientationPolicy(policy: OrientationPolicy) {
        orientationPolicy = policy.copy(
            userAllowedOrientations = copyOfOrNone(policy.userAllowedOrientations),
            sessionOverrideOrientations = policy.sessionOverrideOrientations?.let { copyOfOrNone(it) },
            fallbackOrientations = copyOfOrNone(policy.fallbackOrientations),
        )
        applyOrientationPolicy()
    }

    internal fun applyOrientationSensorValue(orientation: Int) {
        if (orientation == ORIENTATION_UNKNOWN) {
            return
        }

        currentOrientationChangeValue = normalizeToDegrees(orientation)
        applyOrientationPolicy()
    }

    private fun applyOrientationPolicy() {
        val orientations = orientationPolicy.resolvedOrientations().ifEmpty { EnumSet.of(Orientation.UNSPECIFIED) }
        setOrientationTo(currentOrientationChangeValue, orientations)
    }

    private fun setOrientationTo(orientation: Int, conformTo: EnumSet<Orientation>) {
        val orientations = conformTo.ifEmpty { EnumSet.of(Orientation.UNSPECIFIED) }

        if (orientations.contains(Orientation.UNSPECIFIED)) {
            applyRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
            return
        }

        if (orientation == ORIENTATION_UNKNOWN) {
            return
        }

        val adjustedOrientation = normalizeToDegrees(360 - orientation)

        val nearest = orientations
            .map { candidate ->
                candidate to candidate.angleRanges.minOf { range ->
                    range.minOf { angle ->
                        circularDistance(adjustedOrientation, normalizeToDegrees(angle))
                    }
                }
            }
            .minBy { it.second }

        applyRequestedOrientation(nearest.first.activityInfoValue)
    }

    private fun applyRequestedOrientation(targetOrientation: Int) {
        if (activity.requestedOrientation == targetOrientation) {
            return
        }

        Timber.d(
            "Setting requested orientation from %s to %s",
            Orientation.fromActivityInfoValue(activity.requestedOrientation),
            Orientation.fromActivityInfoValue(targetOrientation),
        )

        activity.requestedOrientation = targetOrientation
    }

    private fun normalizeToDegrees(value: Int): Int {
        val normalized = value % FULL_CIRCLE
        return if (normalized < 0) normalized + FULL_CIRCLE else normalized
    }

    private fun circularDistance(a: Int, b: Int): Int {
        val diff = abs(a - b)
        return minOf(diff, FULL_CIRCLE - diff)
    }

    private fun copyOfOrNone(source: EnumSet<Orientation>): EnumSet<Orientation> =
        if (source.isEmpty()) EnumSet.noneOf(Orientation::class.java) else EnumSet.copyOf(source)

    companion object {
        private const val FULL_CIRCLE = 360
    }
}
