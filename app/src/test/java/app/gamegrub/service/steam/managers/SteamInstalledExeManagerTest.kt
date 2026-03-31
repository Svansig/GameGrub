package app.gamegrub.service.steam.managers

import app.gamegrub.data.ConfigInfo
import app.gamegrub.data.DepotInfo
import app.gamegrub.data.LaunchInfo
import app.gamegrub.data.ManifestInfo
import app.gamegrub.data.SteamApp
import app.gamegrub.enums.OS
import app.gamegrub.enums.OSArch
import app.gamegrub.service.steam.managers.SteamInstalledExeManager.ExecutableCandidate
import java.util.EnumSet
import org.junit.Assert.assertEquals
import org.junit.Test

class SteamInstalledExeManagerTest {
    @Test
    fun resolveInstalledExe_returnsEmpty_whenAppInfoMissing() {
        val result = SteamInstalledExeManager.resolveInstalledExe<String>(
            appInfo = null,
            canQueryManifests = true,
            fallbackExecutable = { "fallback.exe" },
            loadManifestCandidates = { _, _ -> emptyList() },
            choosePrimary = { _, _ -> null },
        )

        assertEquals("", result)
    }

    @Test
    fun resolveInstalledExe_returnsFallback_whenManifestQueryUnavailable() {
        val result = SteamInstalledExeManager.resolveInstalledExe<String>(
            appInfo = createApp(),
            canQueryManifests = false,
            fallbackExecutable = { "fallback.exe" },
            loadManifestCandidates = { _, _ -> error("Should not query manifests") },
            choosePrimary = { _, _ -> null },
        )

        assertEquals("fallback.exe", result)
    }

    @Test
    fun resolveInstalledExe_prefersLaunchTargetWhenPresent() {
        val appInfo = createApp(launchExecutable = "Game.exe")

        val result = SteamInstalledExeManager.resolveInstalledExe<String>(
            appInfo = appInfo,
            canQueryManifests = true,
            fallbackExecutable = { "fallback.exe" },
            loadManifestCandidates = { _, _ ->
                listOf(candidate(path = "game.exe", size = 2_000_000L))
            },
            choosePrimary = { _, _ -> null },
        )

        assertEquals("game.exe", result)
    }

    @Test
    fun resolveInstalledExe_usesPrimaryChooserBeforeLargestDepotFallback() {
        val result = SteamInstalledExeManager.resolveInstalledExe<String>(
            appInfo = createApp(launchExecutable = "not_present.exe"),
            canQueryManifests = true,
            fallbackExecutable = { "fallback.exe" },
            loadManifestCandidates = { depotId, _ ->
                if (depotId == 100) {
                    listOf(candidate(path = "candidate_a.exe", size = 10L))
                } else {
                    listOf(candidate(path = "candidate_b.exe", size = 20L))
                }
            },
            choosePrimary = { candidates, _ -> candidates.firstOrNull { it.path == "candidate_b.exe" } },
        )

        assertEquals("candidate_b.exe", result)
    }

    @Test
    fun resolveInstalledExe_usesLargestDepotFallbackWhenChooserReturnsNull() {
        val depots = mapOf(
            100 to createDepot(depotId = 100, manifestSize = 10L),
            200 to createDepot(depotId = 200, manifestSize = 50L),
        )
        val appInfo = createApp(depots = depots, launchExecutable = "missing.exe")

        val result = SteamInstalledExeManager.resolveInstalledExe<String>(
            appInfo = appInfo,
            canQueryManifests = true,
            fallbackExecutable = { "fallback.exe" },
            loadManifestCandidates = { depotId, _ ->
                if (depotId == 200) {
                    listOf(
                        candidate(path = "largest_depot_small.exe", size = 100L),
                        candidate(path = "largest_depot_big.exe", size = 200L),
                    )
                } else {
                    listOf(candidate(path = "other_depot.exe", size = 999L))
                }
            },
            choosePrimary = { _, _ -> null },
        )

        assertEquals("largest_depot_big.exe", result)
    }

    @Test
    fun resolveInstalledExe_returnsFallback_whenNoCandidatesFound() {
        val result = SteamInstalledExeManager.resolveInstalledExe<String>(
            appInfo = createApp(launchExecutable = "missing.exe"),
            canQueryManifests = true,
            fallbackExecutable = { "fallback.exe" },
            loadManifestCandidates = { _, _ -> emptyList<ExecutableCandidate<String>>() },
            choosePrimary = { _, _ -> null },
        )

        assertEquals("fallback.exe", result)
    }

    private fun createApp(
        depots: Map<Int, DepotInfo> = mapOf(100 to createDepot(100, 10L)),
        launchExecutable: String = "game.exe",
    ): SteamApp {
        return SteamApp(
            id = 1,
            name = "Test Game",
            depots = depots,
            config = ConfigInfo(
                installDir = "TestGame",
                launch = listOf(
                    LaunchInfo(
                        executable = launchExecutable,
                        workingDir = "",
                        description = "",
                        type = "",
                        configOS = EnumSet.of(OS.windows),
                        configArch = OSArch.Unknown,
                    ),
                ),
            ),
        )
    }

    private fun createDepot(depotId: Int, manifestSize: Long): DepotInfo {
        return DepotInfo(
            depotId = depotId,
            dlcAppId = Int.MAX_VALUE,
            depotFromApp = 1,
            sharedInstall = false,
            osList = EnumSet.of(OS.windows),
            osArch = OSArch.Unknown,
            manifests = mapOf(
                "public" to ManifestInfo(
                    name = "public",
                    gid = 1L,
                    size = manifestSize,
                    download = manifestSize,
                ),
            ),
            encryptedManifests = emptyMap(),
            language = "english",
        )
    }

    private fun candidate(path: String, size: Long): ExecutableCandidate<String> {
        return ExecutableCandidate(
            source = path,
            path = path,
            totalSize = size,
            hasExecutableFlag = true,
            isStub = false,
        )
    }
}

