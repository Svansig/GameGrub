package app.gamegrub.container.launch.preinstall

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreInstallChainExecutorTest {

    @Test
    fun shouldReportGuestTerminationAsError_returnsFalse_whenExitCodeIsZero() {
        val result = shouldReportGuestTerminationAsError(
            status = 0,
            isExitInProgress = false,
        )

        assertFalse(result)
    }

    @Test
    fun shouldReportGuestTerminationAsError_returnsFalse_whenNonZeroAndExitInProgress() {
        val result = shouldReportGuestTerminationAsError(
            status = 137,
            isExitInProgress = true,
        )

        assertFalse(result)
    }

    @Test
    fun shouldReportGuestTerminationAsError_returnsTrue_whenNonZeroWithoutExitIntent() {
        val result = shouldReportGuestTerminationAsError(
            status = 143,
            isExitInProgress = false,
        )

        assertTrue(result)
    }

    @Test
    fun shouldReportGuestTerminationAsError_returnsFalse_whenExitIntentIsActive_evenForDifferentNonZeroStatus() {
        val result = shouldReportGuestTerminationAsError(
            status = 1,
            isExitInProgress = true,
        )

        assertFalse(result)
    }
}

