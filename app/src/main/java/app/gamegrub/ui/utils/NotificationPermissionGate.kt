package app.gamegrub.ui.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import app.gamegrub.PrefManager

object NotificationPermissionGate {

    fun shouldRequestNotificationPermission(context: Context): Boolean {
        val hasNotificationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED

        return shouldRequestNotificationPermission(
            hasNotificationPermission = hasNotificationPermission,
            hasPrompted = PrefManager.notificationPermissionPrompted,
        )
    }

    internal fun shouldRequestNotificationPermission(
        hasNotificationPermission: Boolean,
        hasPrompted: Boolean,
    ): Boolean {
        return !hasNotificationPermission && !hasPrompted
    }

    fun markPrompted() {
        PrefManager.notificationPermissionPrompted = true
    }
}
