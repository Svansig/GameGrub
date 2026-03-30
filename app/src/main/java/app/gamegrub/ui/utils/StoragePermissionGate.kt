package app.gamegrub.ui.utils

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.provider.Settings
import androidx.core.net.toUri

object StoragePermissionGate {

    fun hasStorageAccess(context: Context, path: String): Boolean {
        return hasStorageAccess(
            isInsideAppSandbox = isInsideAppSandbox(context, path),
            hasManageExternalStorage = Environment.isExternalStorageManager(),
        )
    }

    internal fun hasStorageAccess(
        isInsideAppSandbox: Boolean,
        hasManageExternalStorage: Boolean,
    ): Boolean {
        return isInsideAppSandbox || hasManageExternalStorage
    }

    fun shouldRequestManageStoragePermission(context: Context, path: String): Boolean {
        return shouldRequestManageStoragePermission(
            isInsideAppSandbox = isInsideAppSandbox(context, path),
            hasManageExternalStorage = Environment.isExternalStorageManager(),
        )
    }

    internal fun shouldRequestManageStoragePermission(
        isInsideAppSandbox: Boolean,
        hasManageExternalStorage: Boolean,
    ): Boolean {
        return !isInsideAppSandbox && !hasManageExternalStorage
    }

    fun createManageStoragePermissionIntent(context: Context): Intent? {
        return runCatching {
            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = "package:${context.packageName}".toUri()
            }
        }.getOrNull()
    }

    private fun isInsideAppSandbox(context: Context, path: String): Boolean {
        return path.contains("/Android/data/${context.packageName}") || path.startsWith(context.dataDir.path)
    }
}
