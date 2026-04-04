package app.gamegrub.ui.model

import app.gamegrub.ui.enums.ConnectionState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainViewModelPolicyTest {

    @Test
    fun shouldContinueConnecting_returnsTrue_whenBelowTimeoutAndConnecting() {
        val result = shouldContinueConnecting(
            elapsedSeconds = MainViewModel.CONNECTION_TIMEOUT_SECONDS - 1,
            connectionState = ConnectionState.CONNECTING,
        )

        assertTrue(result)
    }

    @Test
    fun shouldContinueConnecting_returnsFalse_whenAtTimeout() {
        val result = shouldContinueConnecting(
            elapsedSeconds = MainViewModel.CONNECTION_TIMEOUT_SECONDS,
            connectionState = ConnectionState.CONNECTING,
        )

        assertFalse(result)
    }

    @Test
    fun shouldContinueConnecting_returnsFalse_whenNotConnecting() {
        val result = shouldContinueConnecting(
            elapsedSeconds = 0,
            connectionState = ConnectionState.DISCONNECTED,
        )

        assertFalse(result)
    }
}

