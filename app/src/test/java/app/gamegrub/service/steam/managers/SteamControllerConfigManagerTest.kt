package app.gamegrub.service.steam.managers

import app.gamegrub.data.SteamControllerConfigDetail
import java.io.File
import kotlin.io.path.createTempDirectory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SteamControllerConfigManagerTest {
    @Test
    fun selectSteamControllerConfig_prefersDefaultBranchAndControllerPriority() {
        val details = listOf(
            SteamControllerConfigDetail(
                publishedFileId = 100,
                controllerType = "controller_xboxone",
                enabledBranches = listOf("public"),
            ),
            SteamControllerConfigDetail(
                publishedFileId = 200,
                controllerType = "controller_xbox360",
                enabledBranches = listOf("default"),
            ),
        )

        val selected = SteamControllerConfigManager.selectSteamControllerConfig(details)

        assertNotNull(selected)
        assertEquals(200L, selected?.publishedFileId)
    }

    @Test
    fun resolvePathCaseInsensitive_resolvesNestedMixedCasePath() {
        val tempRoot = createTempDirectory("steam-controller-config").toFile()
        val configDir = File(tempRoot, "Config").apply { mkdirs() }
        val configFile = File(configDir, "Gamepad.vdf")
        configFile.writeText("test", Charsets.UTF_8)

        try {
            val resolved = SteamControllerConfigManager.resolvePathCaseInsensitive(
                baseDirPath = tempRoot.path,
                relativePath = "config/gamepad.VDF",
            )

            assertNotNull(resolved)
            assertEquals(configFile.absolutePath, resolved?.absolutePath)
        } finally {
            tempRoot.deleteRecursively()
        }
    }

    @Test
    fun loadConfigFromManifest_returnsReferencedConfigWhenPresent() {
        val tempRoot = createTempDirectory("steam-controller-manifest").toFile()
        val subDir = File(tempRoot, "steam_input").apply { mkdirs() }
        val configFile = File(subDir, "controller_config.vdf")
        configFile.writeText("controller-config", Charsets.UTF_8)

        val manifestFile = File(tempRoot, "manifest.vdf")
        manifestFile.writeText(
            """
            "Action Manifest"
            {
                "configurations"
                {
                    "controller_xboxone"
                    {
                        "0"
                        {
                            "path" "steam_input/controller_config.vdf"
                        }
                    }
                }
            }
            """.trimIndent(),
            Charsets.UTF_8,
        )

        try {
            val result = SteamControllerConfigManager.loadConfigFromManifest(manifestFile)

            assertEquals("controller-config", result)
        } finally {
            tempRoot.deleteRecursively()
        }
    }

    @Test
    fun resolveSteamInputManifestFile_returnsNullWhenPathBlank() {
        val resolved = SteamControllerConfigManager.resolveSteamInputManifestFile(
            appDirPath = "C:/tmp/game",
            manifestPath = "  ",
        )

        assertNull(resolved)
    }
}

