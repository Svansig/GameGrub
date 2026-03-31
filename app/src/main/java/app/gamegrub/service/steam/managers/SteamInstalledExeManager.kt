package app.gamegrub.service.steam.managers

import app.gamegrub.data.ManifestInfo
import app.gamegrub.data.SteamApp
import timber.log.Timber

/**
 * Orchestrates installed executable resolution from app metadata and depot manifests.
 */
object SteamInstalledExeManager {
    data class ExecutableCandidate<T>(
        val source: T,
        val path: String,
        val totalSize: Long,
        val hasExecutableFlag: Boolean,
        val isStub: Boolean,
    )

    fun <T> resolveInstalledExe(
        appInfo: SteamApp?,
        canQueryManifests: Boolean,
        fallbackExecutable: () -> String,
        loadManifestCandidates: (depotId: Int, manifest: ManifestInfo) -> List<ExecutableCandidate<T>>?,
        choosePrimary: (List<ExecutableCandidate<T>>, String) -> ExecutableCandidate<T>?,
    ): String {
        if (appInfo == null) {
            return ""
        }

        val installDir = appInfo.config.installDir.ifEmpty { appInfo.name }
        val depots = appInfo.depots.values.filter { depot ->
            !depot.sharedInstall &&
                (depot.osList.isEmpty() || depot.osList.any { it.name.equals("windows", true) || it.name.equals("none", true) })
        }
        val launchTargets = appInfo.config.launch.map { it.executable.lowercase() }.toSet()

        if (!canQueryManifests) {
            Timber.w("Cannot fetch manifests: steamClient or licenses not available")
            return fallbackExecutable()
        }

        val flagged = mutableListOf<Pair<ExecutableCandidate<T>, Long>>()
        var largestDepotSize = 0L

        for (depot in depots) {
            val manifestInfo = depot.manifests["public"] ?: continue
            if (manifestInfo.size > largestDepotSize) {
                largestDepotSize = manifestInfo.size
            }

            val candidates = loadManifestCandidates(depot.depotId, manifestInfo).orEmpty()

            candidates.firstOrNull { candidate ->
                candidate.path.lowercase() in launchTargets && !candidate.isStub
            }?.let { launchMatch ->
                return launchMatch.path.replace('\\', '/')
            }

            candidates
                .filter { it.hasExecutableFlag || it.path.endsWith(".exe", true) }
                .forEach { candidate -> flagged += candidate to manifestInfo.size }
        }

        val best = choosePrimary(
            flagged.map { it.first }.let { pool ->
                val noStubs = pool.filterNot { it.isStub }
                if (noStubs.isNotEmpty()) noStubs else pool
            },
            installDir.lowercase(),
        )
        if (best != null) {
            return best.path.replace('\\', '/')
        }

        val largestDepotExe = flagged
            .filter { it.second == largestDepotSize }
            .maxByOrNull { it.first.totalSize }
            ?.first
        if (largestDepotExe != null) {
            return largestDepotExe.path.replace('\\', '/')
        }

        Timber.w("No executable found; falling back to install dir")
        return fallbackExecutable()
    }
}
