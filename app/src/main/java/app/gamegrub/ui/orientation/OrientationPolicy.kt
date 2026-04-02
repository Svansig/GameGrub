package app.gamegrub.ui.orientation

import app.gamegrub.ui.enums.Orientation
import java.util.EnumSet

/**
 * Canonical orientation policy contract.
 *
 * `sessionOverrideOrientations` is optional and takes precedence over user settings.
 * `fallbackOrientations` is used when effective orientation inputs are empty.
 *
 * @property userAllowedOrientations The persisted user orientation preference set.
 * @property sessionOverrideOrientations Optional runtime override applied for the active route/session.
 * @property fallbackOrientations Fallback orientations used when both user and override sets are empty.
 */
data class OrientationPolicy(
    val userAllowedOrientations: EnumSet<Orientation>,
    val sessionOverrideOrientations: EnumSet<Orientation>? = null,
    val fallbackOrientations: EnumSet<Orientation> = EnumSet.of(Orientation.UNSPECIFIED),
) {
    /**
     * Resolve the effective orientation set by applying policy precedence.
     *
     * Precedence order:
     * 1) non-empty [sessionOverrideOrientations]
     * 2) non-empty [userAllowedOrientations]
     * 3) [fallbackOrientations]
     *
     * @return A defensive copy of the effective orientation set.
     */
    fun resolvedOrientations(): EnumSet<Orientation> {
        val effective = sessionOverrideOrientations
            ?.takeUnless { it.isEmpty() }
            ?: userAllowedOrientations.takeUnless { it.isEmpty() }
            ?: fallbackOrientations

        return EnumSet.copyOf(effective)
    }

    companion object {
        /**
         * Copy an enum set while safely handling empty input sets.
         *
         * @param source Source enum set to copy.
         * @return A mutable copy of [source], or an empty enum set when [source] is empty.
         */
        private fun copyOfOrNone(source: EnumSet<Orientation>): EnumSet<Orientation> =
            if (source.isEmpty()) EnumSet.noneOf(Orientation::class.java) else EnumSet.copyOf(source)

        /**
         * Create a policy with no session override.
         *
         * @param userAllowedOrientations Persisted user orientation preferences.
         * @return A policy that resolves to user preferences unless they are empty.
         */
        fun default(userAllowedOrientations: EnumSet<Orientation>): OrientationPolicy =
            OrientationPolicy(
                userAllowedOrientations = copyOfOrNone(userAllowedOrientations),
                fallbackOrientations = EnumSet.of(Orientation.UNSPECIFIED),
            )

        /**
         * Create a policy that allows unrestricted rotation for the current session.
         *
         * @param userAllowedOrientations Persisted user orientation preferences retained for future sessions.
         * @return A policy that resolves to [Orientation.UNSPECIFIED] for the active session.
         */
        fun unrestricted(userAllowedOrientations: EnumSet<Orientation>): OrientationPolicy =
            OrientationPolicy(
                userAllowedOrientations = copyOfOrNone(userAllowedOrientations),
                sessionOverrideOrientations = EnumSet.of(Orientation.UNSPECIFIED),
                fallbackOrientations = EnumSet.of(Orientation.UNSPECIFIED),
            )

        /**
         * Create a policy with an explicit session override set.
         *
         * @param userAllowedOrientations Persisted user orientation preferences.
         * @param overrideOrientations Runtime session orientation override.
         * @return A policy that resolves to [overrideOrientations] when non-empty.
         */
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

