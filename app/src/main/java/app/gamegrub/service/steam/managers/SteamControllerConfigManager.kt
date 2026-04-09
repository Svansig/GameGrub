package app.gamegrub.service.steam.managers

import app.gamegrub.data.SteamControllerConfigDetail
import `in`.dragonbra.javasteam.types.KeyValue
import java.io.File
import timber.log.Timber

/**
 * Encapsulates Steam Input controller template and manifest/config file resolution.
 */
object SteamControllerConfigManager {
    fun selectSteamControllerConfig(details: List<SteamControllerConfigDetail>): SteamControllerConfigDetail? {
        if (details.isEmpty()) {
            return null
        }

        val branchPriority = listOf("default", "public")
        val controllerPriority = listOf(
            "controller_xbox360",
            "controller_xboxone",
            "controller_steamcontroller_gordon",
        )

        for (branch in branchPriority) {
            for (controllerType in controllerPriority) {
                val match = details.firstOrNull { detail ->
                    detail.controllerType.equals(controllerType, ignoreCase = true) &&
                        detail.enabledBranches.any { it.equals(branch, ignoreCase = true) }
                }
                if (match != null) {
                    return match
                }
            }
        }

        return null
    }

    fun resolveSteamInputManifestFile(
        appDirPath: String,
        manifestPath: String,
    ): File? {
        val normalizedManifestPath = manifestPath.trim()
        if (normalizedManifestPath.isEmpty()) {
            return null
        }
        return resolvePathCaseInsensitive(appDirPath, normalizedManifestPath)
    }

    fun loadConfigFromManifest(manifestFile: File): String? {
        if (!manifestFile.exists()) {
            return null
        }
        val manifestDirPath = manifestFile.parentFile?.path ?: return null

        val manifestText = manifestFile.readText(Charsets.UTF_8)
        val configText = try {
            parseManifestForConfig(manifestDirPath, manifestText)
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse Steam Input manifest config at ${manifestFile.path}")
            return null
        }
        return configText ?: manifestText
    }

    fun parseManifestForConfig(
        manifestDirPath: String,
        manifestText: String,
    ): String? {
        return try {
            val kv = KeyValue.loadFromString(manifestText) ?: return null
            val actionManifest = if (kv.name?.equals("Action Manifest", ignoreCase = true) == true) {
                kv
            } else {
                kv["Action Manifest"]
            }
            if (actionManifest === KeyValue.INVALID) {
                return null
            }

            val configs = actionManifest["configurations"]
            if (configs === KeyValue.INVALID || configs.children.isEmpty()) {
                throw IllegalStateException("No configurations found in Action Manifest")
            }

            val preferredControllers = listOf(
                "controller_xboxone",
                "controller_steamcontroller_gordon",
                "controller_generic",
                "controller_xbox360",
            )

            for (controllerType in preferredControllers) {
                val controllerBlock = configs[controllerType]
                if (controllerBlock === KeyValue.INVALID) {
                    continue
                }

                for (entry in controllerBlock.children) {
                    val pathNode = entry["path"]
                    val configPath = pathNode.asString().orEmpty()
                    if (pathNode === KeyValue.INVALID || configPath.isEmpty()) {
                        continue
                    }

                    val configFile = resolvePathCaseInsensitive(manifestDirPath, configPath) ?: continue
                    return configFile.readText(Charsets.UTF_8)
                }
            }

            throw IllegalStateException("No valid controller configuration found in Action Manifest")
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse Steam Input manifest config")
            null
        }
    }

    fun resolvePathCaseInsensitive(
        baseDirPath: String,
        relativePath: String,
    ): File? {
        val directFile = File(baseDirPath, relativePath)
        if (directFile.exists()) {
            return directFile
        }

        var currentDir = File(baseDirPath)
        if (!currentDir.exists() || !currentDir.isDirectory) {
            return null
        }

        val segments = relativePath.split('/', '\\').filter { it.isNotEmpty() }
        for ((index, segment) in segments.withIndex()) {
            val entries = currentDir.listFiles() ?: return null
            val matched = entries.firstOrNull {
                it.name.equals(segment, ignoreCase = true)
            } ?: return null

            if (index == segments.lastIndex) {
                return matched
            }

            if (!matched.isDirectory) {
                return null
            }
            currentDir = matched
        }

        return null
    }
}
