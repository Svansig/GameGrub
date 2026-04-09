package app.gamegrub.session

import app.gamegrub.session.model.SessionPlan
import java.io.File
import kotlinx.serialization.json.Json
import timber.log.Timber

class SessionArtifactWriter(
    private val outputDir: File,
) {
    private val json = Json { prettyPrint = true }

    fun writeArtifacts(sessionPlan: SessionPlan): Boolean {
        return try {
            outputDir.mkdirs()

            writeMountsJson(sessionPlan)
            writeEnvJson(sessionPlan)
            writeLaunchJson(sessionPlan)

            Timber.d("Wrote session artifacts to ${outputDir.absolutePath}")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to write session artifacts")
            false
        }
    }

    private fun writeMountsJson(sessionPlan: SessionPlan): File {
        val file = File(outputDir, "mounts.json")
        val mountsData = mapOf(
            "sessionId" to sessionPlan.sessionId,
            "baseMount" to sessionPlan.mountPlan.baseMount,
            "runtimeMount" to sessionPlan.mountPlan.runtimeMount,
            "driverMount" to sessionPlan.mountPlan.driverMount,
            "containerPrefixMount" to sessionPlan.mountPlan.containerPrefixMount,
            "containerInstallMount" to sessionPlan.mountPlan.containerInstallMount,
            "containerSavesMount" to sessionPlan.mountPlan.containerSavesMount,
            "containerCacheMount" to sessionPlan.mountPlan.containerCacheMount,
            "tempDirMount" to sessionPlan.mountPlan.tempDirMount,
            "bindMounts" to sessionPlan.mountPlan.bindMounts.map {
                mapOf(
                    "source" to it.source,
                    "target" to it.target,
                    "readOnly" to it.readOnly,
                )
            },
        )
        file.writeText(json.encodeToString(mountsData))
        return file
    }

    private fun writeEnvJson(sessionPlan: SessionPlan): File {
        val file = File(outputDir, "env.json")
        val envData = mapOf(
            "sessionId" to sessionPlan.sessionId,
            "environmentVariables" to sessionPlan.envPlan.environmentVariables,
            "pathAdditions" to sessionPlan.envPlan.pathAdditions,
            "winePrefix" to sessionPlan.envPlan.winePrefix,
            "wineRuntime" to sessionPlan.envPlan.wineRuntime,
            "dxvkStateCache" to sessionPlan.envPlan.dxvkStateCache,
            "mesaShaderCache" to sessionPlan.envPlan.mesaShaderCache,
            "xdgCacheHome" to sessionPlan.envPlan.xdgCacheHome,
        )
        file.writeText(json.encodeToString(envData))
        return file
    }

    private fun writeLaunchJson(sessionPlan: SessionPlan): File {
        val file = File(outputDir, "launch.json")
        val compositionData = when (val comp = sessionPlan.composition) {
            is app.gamegrub.session.model.SessionComposition.Full -> mapOf(
                "type" to "Full",
                "baseId" to comp.base.id,
                "baseVersion" to comp.base.version,
                "runtimeId" to comp.runtime.id,
                "runtimeVersion" to comp.runtime.version,
                "driverId" to comp.driver?.id,
                "containerId" to comp.container.id,
            )

            is app.gamegrub.session.model.SessionComposition.Partial -> mapOf(
                "type" to "Partial",
                "containerId" to comp.container.id,
                "missingComponents" to comp.missingComponents,
            )

            is app.gamegrub.session.model.SessionComposition.Failed -> mapOf(
                "type" to "Failed",
                "reason" to comp.reason,
                "missingComponents" to comp.missingComponents,
            )
        }

        val launchData = mapOf(
            "sessionId" to sessionPlan.sessionId,
            "metadata" to mapOf(
                "gameId" to sessionPlan.metadata.gameId,
                "gameTitle" to sessionPlan.metadata.gameTitle,
                "gamePlatform" to sessionPlan.metadata.gamePlatform,
                "createdAt" to sessionPlan.metadata.createdAt,
                "launchArgs" to sessionPlan.metadata.launchArgs,
            ),
            "state" to sessionPlan.state.name,
            "composition" to compositionData,
            "cacheHandles" to sessionPlan.cacheHandles.map {
                mapOf(
                    "cacheId" to it.cacheId,
                    "cacheType" to it.cacheType,
                    "mountPath" to it.mountPath,
                    "key" to it.key,
                )
            },
        )
        file.writeText(json.encodeToString(launchData))
        return file
    }

    fun readSessionPlan(sessionId: String): SessionPlan? {
        val launchFile = File(outputDir, "launch.json")
        return try {
            if (launchFile.exists()) {
                json.decodeFromString<SessionPlan>(launchFile.readText())
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to read session plan: $sessionId")
            null
        }
    }

    companion object {
        fun forSession(sessionDir: File): SessionArtifactWriter {
            return SessionArtifactWriter(sessionDir)
        }
    }
}
