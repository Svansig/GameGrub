package app.gamegrub.service.steam.managers

import app.gamegrub.data.SteamControllerConfigDetail
import java.io.File
import kotlin.io.path.createTempDirectory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SteamControllerWorkshopDownloadManagerTest {
    @Test
    fun tryDownloadWorkshopControllerConfig_returnsFalse_whenTemplateDoesNotRequireWorkshop() {
        val result = SteamControllerWorkshopDownloadManager.tryDownloadWorkshopControllerConfig(
            templateIndex = 2,
            configDetails = listOf(SteamControllerConfigDetail(publishedFileId = 1L)),
            appDirPath = createTempDirectory("steam-workshop").toFile().path,
            configFileName = "steam_controller.vdf",
            requiresWorkshopDownload = { false },
            selectConfig = { it.firstOrNull() },
            fetchPublishedFileDetailsJson = { "" },
            downloadFileBytes = { byteArrayOf() },
        )

        assertFalse(result)
    }

    @Test
    fun tryDownloadWorkshopControllerConfig_returnsFalse_whenFileUrlMissing() {
        val tmpDir = createTempDirectory("steam-workshop").toFile()
        try {
            val result = SteamControllerWorkshopDownloadManager.tryDownloadWorkshopControllerConfig(
                templateIndex = 1,
                configDetails = listOf(SteamControllerConfigDetail(publishedFileId = 42L)),
                appDirPath = tmpDir.path,
                configFileName = "steam_controller.vdf",
                requiresWorkshopDownload = { true },
                selectConfig = { it.firstOrNull() },
                fetchPublishedFileDetailsJson = {
                    """
                    {
                      "response": {
                        "result": 1,
                        "resultcount": 1,
                        "publishedfiledetails": [ { "file_url": "" } ]
                      }
                    }
                    """.trimIndent()
                },
                downloadFileBytes = { byteArrayOf() },
            )

            assertFalse(result)
            assertFalse(File(tmpDir, "steam_controller.vdf").exists())
        } finally {
            tmpDir.deleteRecursively()
        }
    }

    @Test
    fun tryDownloadWorkshopControllerConfig_returnsFalse_whenDownloadFails() {
        val tmpDir = createTempDirectory("steam-workshop").toFile()
        try {
            val result = SteamControllerWorkshopDownloadManager.tryDownloadWorkshopControllerConfig(
                templateIndex = 1,
                configDetails = listOf(SteamControllerConfigDetail(publishedFileId = 7L)),
                appDirPath = tmpDir.path,
                configFileName = "steam_controller.vdf",
                requiresWorkshopDownload = { true },
                selectConfig = { it.firstOrNull() },
                fetchPublishedFileDetailsJson = {
                    """
                    {
                      "response": {
                        "result": 1,
                        "resultcount": 1,
                        "publishedfiledetails": [ { "file_url": "https://example.com/file.vdf" } ]
                      }
                    }
                    """.trimIndent()
                },
                downloadFileBytes = { null },
            )

            assertFalse(result)
        } finally {
            tmpDir.deleteRecursively()
        }
    }

    @Test
    fun tryDownloadWorkshopControllerConfig_writesFile_whenAllInputsValid() {
        val tmpDir = createTempDirectory("steam-workshop").toFile()
        try {
            val result = SteamControllerWorkshopDownloadManager.tryDownloadWorkshopControllerConfig(
                templateIndex = 1,
                configDetails = listOf(SteamControllerConfigDetail(publishedFileId = 11L)),
                appDirPath = tmpDir.path,
                configFileName = "steam_controller.vdf",
                requiresWorkshopDownload = { it == 1 },
                selectConfig = { it.firstOrNull() },
                fetchPublishedFileDetailsJson = {
                    """
                    {
                      "response": {
                        "result": 1,
                        "resultcount": 1,
                        "publishedfiledetails": [ { "file_url": "https://example.com/file.vdf" } ]
                      }
                    }
                    """.trimIndent()
                },
                downloadFileBytes = { "test-content".toByteArray(Charsets.UTF_8) },
            )

            assertTrue(result)
            val fileText = File(tmpDir, "steam_controller.vdf").readText(Charsets.UTF_8)
            assertEquals("test-content", fileText)
        } finally {
            tmpDir.deleteRecursively()
        }
    }
}

