package app.gamegrub.ui.launch

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameLaunchRetryPolicyTest {

    @Test
    fun shouldRetrySyncInProgress_returnsTrue_whenOverrideEnabledAndBelowLimit() {
        val result = shouldRetrySyncInProgress(
            useTemporaryOverride = true,
            retryCount = 4,
        )

        assertTrue(result)
    }

    @Test
    fun shouldRetrySyncInProgress_returnsFalse_whenRetryCountAtLimit() {
        val result = shouldRetrySyncInProgress(
            useTemporaryOverride = true,
            retryCount = 5,
        )

        assertFalse(result)
    }

    @Test
    fun shouldRetrySyncInProgress_returnsFalse_whenOverrideDisabled() {
        val result = shouldRetrySyncInProgress(
            useTemporaryOverride = false,
            retryCount = 0,
        )

        assertFalse(result)
    }
}
