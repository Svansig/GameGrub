package app.gamegrub.ui.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StoragePermissionGateTest {

    @Test
    fun hasStorageAccess_returnsTrue_whenPathIsInsideSandbox() {
        val hasAccess = StoragePermissionGate.hasStorageAccess(
            isInsideAppSandbox = true,
            hasManageExternalStorage = false,
        )

        assertTrue(hasAccess)
    }

    @Test
    fun hasStorageAccess_returnsTrue_whenManageExternalStorageGranted() {
        val hasAccess = StoragePermissionGate.hasStorageAccess(
            isInsideAppSandbox = false,
            hasManageExternalStorage = true,
        )

        assertTrue(hasAccess)
    }

    @Test
    fun hasStorageAccess_returnsFalse_whenOutsideSandboxAndNoPermission() {
        val hasAccess = StoragePermissionGate.hasStorageAccess(
            isInsideAppSandbox = false,
            hasManageExternalStorage = false,
        )

        assertFalse(hasAccess)
    }

    @Test
    fun shouldRequestManageStoragePermission_returnsFalse_whenInsideSandbox() {
        val shouldRequest = StoragePermissionGate.shouldRequestManageStoragePermission(
            isInsideAppSandbox = true,
            hasManageExternalStorage = false,
        )

        assertFalse(shouldRequest)
    }

    @Test
    fun shouldRequestManageStoragePermission_returnsFalse_whenPermissionAlreadyGranted() {
        val shouldRequest = StoragePermissionGate.shouldRequestManageStoragePermission(
            isInsideAppSandbox = false,
            hasManageExternalStorage = true,
        )

        assertFalse(shouldRequest)
    }

    @Test
    fun shouldRequestManageStoragePermission_returnsTrue_whenOutsideSandboxWithoutPermission() {
        val shouldRequest = StoragePermissionGate.shouldRequestManageStoragePermission(
            isInsideAppSandbox = false,
            hasManageExternalStorage = false,
        )

        assertTrue(shouldRequest)
    }
}

