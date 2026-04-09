package app.gamegrub.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.OrientationEventListener
import android.view.OrientationEventListener.ORIENTATION_UNKNOWN
import app.gamegrub.ui.enums.Orientation
import app.gamegrub.ui.orientation.OrientationPolicy
import java.util.EnumSet
import kotlin.math.abs
import timber.log.Timber

/**
 * Manages device orientation changes and applies orientation constraints.
 * Uses orientation sensor to detect device rotation and applies user-defined
 * orientation preferences.
 *
 * This class is the sole writer for [Activity.requestedOrientation] and applies
 * route/session policy through [OrientationPolicy].
 *
 * @property activity Host activity that receives orientation requests.
 */
class OrientationManager(private val activity: Activity) {

    /**
     * Active sensor listener instance while orientation sensing is enabled.
     */
    private var orientationSensorListener: OrientationEventListener? = null

    /**
     * Last known orientation sensor value in degrees.
     *
     * Stored as [OrientationEventListener.ORIENTATION_UNKNOWN] until a concrete value is observed.
     */
    private var currentOrientationChangeValue: Int = ORIENTATION_UNKNOWN

    /**
     * Active orientation policy used to resolve the allowed orientation set.
     */
    private var orientationPolicy: OrientationPolicy = OrientationPolicy.default(EnumSet.of(Orientation.UNSPECIFIED))

    /**
     * Start listening to orientation sensor.
     *
     * This method is idempotent; repeated calls while active are ignored.
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
     *
     * This method is idempotent; repeated calls while inactive are ignored.
     */
    fun stopOrientator() {
        val listener = orientationSensorListener ?: return
        listener.disable()
        orientationSensorListener = null
    }

    /**
     * Update the active orientation policy and reapply orientation constraints immediately.
     *
     * @param policy Orientation policy contract to apply.
     */
    fun setOrientationPolicy(policy: OrientationPolicy) {
        orientationPolicy = policy.copy(
            userAllowedOrientations = copyOfOrNone(policy.userAllowedOrientations),
            sessionOverrideOrientations = policy.sessionOverrideOrientations?.let { copyOfOrNone(it) },
            fallbackOrientations = copyOfOrNone(policy.fallbackOrientations),
        )
        applyOrientationPolicy()
    }

    /**
     * Apply a raw orientation sensor reading to the current policy.
     *
     * Unknown values are ignored to prevent transient sensor noise from causing orientation churn.
     *
     * @param orientation Sensor value in degrees from `0..359` or `ORIENTATION_UNKNOWN`.
     */
    internal fun applyOrientationSensorValue(orientation: Int) {
        if (orientation == ORIENTATION_UNKNOWN) {
            return
        }

        currentOrientationChangeValue = normalizeToDegrees(orientation)
        applyOrientationPolicy()
    }

    /**
     * Resolve effective policy orientations and project them to activity orientation.
     */
    private fun applyOrientationPolicy() {
        val orientations = orientationPolicy.resolvedOrientations().ifEmpty { EnumSet.of(Orientation.UNSPECIFIED) }
        setOrientationTo(currentOrientationChangeValue, orientations)
    }

    /**
     * Select and apply the nearest allowed orientation for the current sensor value.
     *
     * @param orientation Last known orientation sensor reading.
     * @param conformTo Candidate orientations derived from policy resolution.
     */
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

    /**
     * Apply a requested activity orientation only when the target differs from current value.
     *
     * @param targetOrientation `ActivityInfo.SCREEN_ORIENTATION_*` value to apply.
     */
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

    /**
     * Normalize arbitrary degree values into `[0, 359]`.
     *
     * @param value Raw degree value.
     * @return Normalized degree value.
     */
    private fun normalizeToDegrees(value: Int): Int {
        val normalized = value % FULL_CIRCLE
        return if (normalized < 0) normalized + FULL_CIRCLE else normalized
    }

    /**
     * Compute shortest angular distance between two orientations.
     *
     * @param a First angle in degrees.
     * @param b Second angle in degrees.
     * @return Circular distance between [a] and [b].
     */
    private fun circularDistance(a: Int, b: Int): Int {
        val diff = abs(a - b)
        return minOf(diff, FULL_CIRCLE - diff)
    }

    /**
     * Copy an enum set while safely handling empty input sets.
     *
     * @param source Source orientation set.
     * @return Defensive copy of [source], or an empty orientation enum set.
     */
    private fun copyOfOrNone(source: EnumSet<Orientation>): EnumSet<Orientation> =
        if (source.isEmpty()) EnumSet.noneOf(Orientation::class.java) else EnumSet.copyOf(source)

    companion object {
        /**
         * Number of degrees in a full circle.
         */
        private const val FULL_CIRCLE = 360
    }
}
