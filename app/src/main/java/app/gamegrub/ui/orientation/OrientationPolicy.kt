package app.gamegrub.ui.orientation

import app.gamegrub.ui.enums.Orientation
import java.util.EnumSet

/**
 * Canonical orientation policy contract.
 *
 * `sessionOverrideOrientations` is optional and takes precedence over user settings.
 * `fallbackOrientations` is used when effective orientation inputs are empty.
 */
data class OrientationPolicy(
    val userAllowedOrientations: EnumSet<Orientation>,
    val sessionOverrideOrientations: EnumSet<Orientation>? = null,
    val fallbackOrientations: EnumSet<Orientation> = EnumSet.of(Orientation.UNSPECIFIED),
) {
    fun resolvedOrientations(): EnumSet<Orientation> {
        val effective = sessionOverrideOrientations
            ?.takeUnless { it.isEmpty() }
            ?: userAllowedOrientations.takeUnless { it.isEmpty() }
            ?: fallbackOrientations

        return EnumSet.copyOf(effective)
    }

    companion object {
        private fun copyOfOrNone(source: EnumSet<Orientation>): EnumSet<Orientation> =
            if (source.isEmpty()) EnumSet.noneOf(Orientation::class.java) else EnumSet.copyOf(source)

        fun default(userAllowedOrientations: EnumSet<Orientation>): OrientationPolicy =
            OrientationPolicy(
                userAllowedOrientations = copyOfOrNone(userAllowedOrientations),
                fallbackOrientations = EnumSet.of(Orientation.UNSPECIFIED),
            )

        fun unrestricted(userAllowedOrientations: EnumSet<Orientation>): OrientationPolicy =
            OrientationPolicy(
                userAllowedOrientations = copyOfOrNone(userAllowedOrientations),
                sessionOverrideOrientations = EnumSet.of(Orientation.UNSPECIFIED),
                fallbackOrientations = EnumSet.of(Orientation.UNSPECIFIED),
            )

        fun forSessionOverride(
            userAllowedOrientations: EnumSet<Orientation>,
            overrideOrientations: EnumSet<Orientation>,
        ): OrientationPolicy = OrientationPolicy(
            userAllowedOrientations = copyOfOrNone(userAllowedOrientations),
            sessionOverrideOrientations = copyOfOrNone(overrideOrientations),
            fallbackOrientations = EnumSet.of(Orientation.UNSPECIFIED),
        )
    }
}

