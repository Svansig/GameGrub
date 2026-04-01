package app.gamegrub.ui

import android.app.Activity
import android.view.OrientationEventListener
import app.gamegrub.ui.enums.Orientation
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

    private var currentOrientationChangeValue: Int = 0
    private var availableOrientations: EnumSet<Orientation> = EnumSet.of(Orientation.UNSPECIFIED)

    /**
     * Start listening to orientation sensor.
     */
    fun startOrientator() {
        orientationSensorListener = object : OrientationEventListener(activity) {
            override fun onOrientationChanged(orientation: Int) {
                currentOrientationChangeValue = if (orientation != ORIENTATION_UNKNOWN) {
                    orientation
                } else {
                    currentOrientationChangeValue
                }
                setOrientationTo(currentOrientationChangeValue, availableOrientations)
            }
        }

        orientationSensorListener?.takeIf { it.canDetectOrientation() }?.enable()
    }

    /**
     * Stop listening to orientation sensor.
     */
    fun stopOrientator() {
        orientationSensorListener?.disable()
        orientationSensorListener = null
    }

    /**
     * Update available orientations and reapply orientation constraints.
     */
    fun setAllowedOrientations(orientations: EnumSet<Orientation>) {
        availableOrientations = orientations
        setOrientationTo(currentOrientationChangeValue, availableOrientations)
    }

    private fun setOrientationTo(orientation: Int, conformTo: EnumSet<Orientation>) {
        val adjustedOrientation = 360 - orientation

        val orientations = conformTo.ifEmpty { EnumSet.of(Orientation.UNSPECIFIED) }

        var inRange = orientations
            .filter { it.angleRanges.any { range -> range.contains(adjustedOrientation) } }
            .toTypedArray()

        if (inRange.isEmpty()) {
            inRange = orientations.toTypedArray()
        }

        val distances = orientations.map {
            it to it.angleRanges.minOf { range ->
                range.minOf { angle ->
                    if (adjustedOrientation == 0 || adjustedOrientation == 360) {
                        minOf(abs(angle), abs(angle - 360))
                    } else {
                        abs(angle - adjustedOrientation)
                    }
                }
            }
        }

        val nearest = distances.minBy { it.second }

        val currentOrientationDist = distances
            .firstOrNull { it.first.activityInfoValue == activity.requestedOrientation }
            ?.second
            ?: Int.MAX_VALUE

        if (activity.requestedOrientation != nearest.first.activityInfoValue &&
            currentOrientationDist > nearest.second
        ) {
            Timber.d(
                "%snull",
                "$adjustedOrientation => currentOrientation(" +
                    "${Orientation.fromActivityInfoValue(activity.requestedOrientation)}) " +
                    "!= nearestOrientation(${nearest.first}) && ",
            )

            activity.requestedOrientation = nearest.first.activityInfoValue
        }
    }
}
