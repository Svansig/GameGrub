package app.gamegrub.service

import app.gamegrub.data.SteamLibraryApp
import app.gamegrub.enums.Marker
import app.gamegrub.storage.StorageManager
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InstalledGamesStartupValidatorTest {
    @Test
    fun resolveInstalledPathFromMarkers_completeMarkerWithoutInProgress_returnsPath() {
        val root = Files.createTempDirectory("install-validation-test")
        try {
            val installPath = root.resolve("gameA").toFile().apply { mkdirs() }.absolutePath
            StorageManager.addMarker(installPath, Marker.DOWNLOAD_COMPLETE_MARKER)

            val resolved = resolveInstalledPathFromMarkers(listOf(installPath))

            assertEquals(installPath, resolved)
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun resolveInstalledPathFromMarkers_inProgressMarkerPresent_returnsNull() {
        val root = Files.createTempDirectory("install-validation-test")
        try {
            val installPath = root.resolve("gameA").toFile().apply { mkdirs() }.absolutePath
            StorageManager.addMarker(installPath, Marker.DOWNLOAD_COMPLETE_MARKER)
            StorageManager.addMarker(installPath, Marker.DOWNLOAD_IN_PROGRESS_MARKER)

            val resolved = resolveInstalledPathFromMarkers(listOf(installPath))

            assertNull(resolved)
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun resolveInstalledPathFromMarkers_usesFirstValidDistinctPath() {
        val root = Files.createTempDirectory("install-validation-test")
        try {
            val invalidPath = root.resolve("invalid").toFile().apply { mkdirs() }.absolutePath
            val validPath = root.resolve("valid").toFile().apply { mkdirs() }.absolutePath
            StorageManager.addMarker(invalidPath, Marker.DOWNLOAD_IN_PROGRESS_MARKER)
            StorageManager.addMarker(validPath, Marker.DOWNLOAD_COMPLETE_MARKER)

            val resolved = resolveInstalledPathFromMarkers(
                listOf(
                    " ",
                    invalidPath,
                    validPath,
                    validPath,
                ),
            )

            assertEquals(validPath, resolved)
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun resolveInstalledPathFromMarkers_markerlessDirectory_returnsPath() {
        val root = Files.createTempDirectory("install-validation-test")
        try {
            val legacyPath = root.resolve("legacy").toFile().apply { mkdirs() }.absolutePath

            val resolved = resolveInstalledPathFromMarkers(listOf(legacyPath))

            assertEquals(legacyPath, resolved)
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun isSteamLibraryAppInstalled_completeMarkerPresent_returnsTrue() {
        val root = Files.createTempDirectory("steam-install-validation-test")
        try {
            val basePath = root.toFile().absolutePath
            val appDir = root.resolve("HalfLife").toFile().apply { mkdirs() }.absolutePath
            StorageManager.addMarker(appDir, Marker.DOWNLOAD_COMPLETE_MARKER)

            val app = SteamLibraryApp(
                id = 70,
                name = "HalfLife",
                installDir = "HalfLife",
            )

            val installed = isSteamLibraryAppInstalled(
                app = app,
                installPaths = listOf(basePath),
            )

            assertEquals(true, installed)
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun isSteamLibraryAppInstalled_inProgressMarkerPresent_returnsFalse() {
        val root = Files.createTempDirectory("steam-install-validation-test")
        try {
            val basePath = root.toFile().absolutePath
            val appDir = root.resolve("HalfLife").toFile().apply { mkdirs() }.absolutePath
            StorageManager.addMarker(appDir, Marker.DOWNLOAD_COMPLETE_MARKER)
            StorageManager.addMarker(appDir, Marker.DOWNLOAD_IN_PROGRESS_MARKER)

            val app = SteamLibraryApp(
                id = 70,
                name = "HalfLife",
                installDir = "HalfLife",
            )

            val installed = isSteamLibraryAppInstalled(
                app = app,
                installPaths = listOf(basePath),
            )

            assertEquals(false, installed)
        } finally {
            root.toFile().deleteRecursively()
        }
    }
}


