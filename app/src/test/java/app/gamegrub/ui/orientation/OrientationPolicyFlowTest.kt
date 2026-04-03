package app.gamegrub.ui.orientation

import app.gamegrub.ui.enums.Orientation
import java.util.EnumSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OrientationPolicyFlowTest {

    @Test
    fun resolvedOrientations_prefersSessionOverrideOverUserAllowed() {
        val policy = OrientationPolicy(
            userAllowedOrientations = EnumSet.of(Orientation.LANDSCAPE, Orientation.REVERSE_LANDSCAPE),
            sessionOverrideOrientations = EnumSet.of(Orientation.PORTRAIT),
        )

        val resolved = policy.resolvedOrientations()

        assertEquals(EnumSet.of(Orientation.PORTRAIT), resolved)
    }

    @Test
    fun resolvedOrientations_fallsBackWhenUserAndOverrideAreEmpty() {
        val policy = OrientationPolicy(
            userAllowedOrientations = EnumSet.noneOf(Orientation::class.java),
            sessionOverrideOrientations = EnumSet.noneOf(Orientation::class.java),
            fallbackOrientations = EnumSet.of(Orientation.UNSPECIFIED),
        )

        val resolved = policy.resolvedOrientations()

        assertEquals(EnumSet.of(Orientation.UNSPECIFIED), resolved)
    }

    @Test
    fun defaultFactory_preservesUserAllowedOrientations() {
        val userAllowed = EnumSet.of(Orientation.LANDSCAPE, Orientation.REVERSE_LANDSCAPE)

        val policy = OrientationPolicy.default(userAllowed)

        assertEquals(userAllowed, policy.resolvedOrientations())
        assertTrue(policy.sessionOverrideOrientations == null)
    }
}

