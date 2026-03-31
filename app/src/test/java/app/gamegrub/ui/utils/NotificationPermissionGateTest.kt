package app.gamegrub.ui.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPermissionGateTest {

    @Test
    fun shouldRequestNotificationPermission_returnsTrue_whenNotGrantedAndNotPrompted() {
        val shouldRequest = NotificationPermissionGate.shouldRequestNotificationPermission(
            hasNotificationPermission = false,
            hasPrompted = false,
        )

        assertTrue(shouldRequest)
    }

    @Test
    fun shouldRequestNotificationPermission_returnsFalse_whenAlreadyGranted() {
        val shouldRequest = NotificationPermissionGate.shouldRequestNotificationPermission(
            hasNotificationPermission = true,
            hasPrompted = false,
        )

        assertFalse(shouldRequest)
    }

    @Test
    fun shouldRequestNotificationPermission_returnsFalse_whenAlreadyPrompted() {
        val shouldRequest = NotificationPermissionGate.shouldRequestNotificationPermission(
            hasNotificationPermission = false,
            hasPrompted = true,
        )

        assertFalse(shouldRequest)
    }
}

