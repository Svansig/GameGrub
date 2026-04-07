package app.gamegrub.container.launch.command

import app.gamegrub.data.GameSource
import app.gamegrub.domain.customgame.ExecutableSelectionUtils
import app.gamegrub.service.amazon.AmazonSdkManager
import app.gamegrub.service.amazon.AmazonService
import com.winlator.core.envvars.EnvVars
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import timber.log.Timber
import java.io.File

/**
 * Builds Amazon launch commands and applies Fuel/FuelPump environment setup.
 */
internal object AmazonLaunchCommandBuilder : BaseLaunchCommandBuilder() {
    override val gameSource: GameSource = GameSource.AMAZON

    override fun buildStoreCommand(context: LaunchCommandContext): String {
        val appIdInt = context.gameId
        val productId = AmazonService.getProductIdByAppId(appIdInt)
        Timber.tag("XServerScreen").i("Launching Amazon game: appId=$appIdInt, productId=$productId")

        val installPath = AmazonService.getInstallPathByAppIdSync(appIdInt)
        if (installPath.isNullOrEmpty()) {
            Timber.tag("XServerScreen").e("Cannot launch: Amazon game not installed")
            return "\"explorer.exe\""
        }

        val fuelManifest = parseFuelManifest(installPath)
        val validatedFuelCommand = validateFuelCommand(installPath, fuelManifest.command)

        val resolvedRelativePath = resolveExecutableRelativePath(
            installPath = installPath,
            container = context.container,
            fuelCommand = validatedFuelCommand,
        ) ?: return "\"explorer.exe\""

        val amazonCommand = "A:\\${resolvedRelativePath.replace("/", "\\")}"
        context.guestProgramLauncherComponent.workingDir = File(
            resolveWorkingDirectory(
                installPath = installPath,
                resolvedRelativePath = resolvedRelativePath,
                fuelCommand = validatedFuelCommand,
                fuelWorkingDir = fuelManifest.workingSubdirOverride,
            ),
        )

        applyFuelEnvironmentVariables(context.envVars, appIdInt, productId)
        deployAmazonSdk(context.context, context.container)

        return buildAmazonLaunchCommand(amazonCommand, fuelManifest.args)
    }

    private fun parseFuelManifest(installPath: String): FuelManifest {
        val fuelFile = File(installPath, "fuel.json")
        if (!fuelFile.exists()) {
            Timber.tag("XServerScreen").d("No fuel.json found at ${fuelFile.path}, using heuristic")
            return FuelManifest()
        }

        return try {
            val json = JSONObject(fuelFile.readText())
            val main = json.optJSONObject("Main")
            val command = main?.optString("Command", "")?.takeIf { it.isNotEmpty() }
            val workingSubdirOverride = main?.optString("WorkingSubdirOverride", "")?.takeIf { it.isNotEmpty() }
            val argsArray = main?.optJSONArray("Args")
            val args = if (argsArray == null) {
                emptyList()
            } else {
                (0 until argsArray.length()).mapNotNull { argsArray.optString(it) }
            }

            Timber.tag("XServerScreen").i(
                "fuel.json parsed: command=$command, args=$args, workingDir=$workingSubdirOverride",
            )
            FuelManifest(command = command, workingSubdirOverride = workingSubdirOverride, args = args)
        } catch (e: Exception) {
            Timber.tag("XServerScreen").w(e, "Failed to parse fuel.json, falling back to heuristic")
            FuelManifest()
        }
    }

    private fun validateFuelCommand(installPath: String, fuelCommand: String?): String? {
        if (fuelCommand == null) {
            return null
        }

        val exeFile = File(installPath, fuelCommand.replace("\\", "/"))
        if (exeFile.exists()) {
            return fuelCommand
        }

        Timber.tag("XServerScreen").w("fuel.json executable not found: ${exeFile.path}, falling back to heuristic")
        return null
    }

    private fun resolveExecutableRelativePath(
        installPath: String,
        container: com.winlator.container.Container,
        fuelCommand: String?,
    ): String? {
        var resolvedRelativePath = container.executablePath
        if (resolvedRelativePath.isNotEmpty()) {
            Timber.tag("XServerScreen").i("Using cached Amazon executablePath: $resolvedRelativePath")
            return resolvedRelativePath
        }

        resolvedRelativePath = if (fuelCommand != null) {
            fuelCommand.replace("\\", "/")
        } else {
            val exeFile = ExecutableSelectionUtils.choosePrimaryExeFromDisk(
                installDir = File(installPath),
                gameName = File(installPath).name,
            )
            if (exeFile == null) {
                Timber.tag("XServerScreen").e("Cannot find executable for Amazon game")
                return null
            }
            Timber.tag("XServerScreen").d("Heuristic selected exe: ${exeFile.path}")
            exeFile.relativeTo(File(installPath)).path
        }

        container.executablePath = resolvedRelativePath
        container.saveData()
        return resolvedRelativePath
    }

    private fun resolveWorkingDirectory(
        installPath: String,
        resolvedRelativePath: String,
        fuelCommand: String?,
        fuelWorkingDir: String?,
    ): String {
        if (fuelCommand != null &&
            fuelWorkingDir != null &&
            resolvedRelativePath.replace("\\", "/") == fuelCommand.replace("\\", "/")
        ) {
            return installPath + "/" + fuelWorkingDir.replace("\\", "/")
        }

        val exeDir = resolvedRelativePath.substringBeforeLast("/", "")
        return if (exeDir.isNotEmpty()) "$installPath/$exeDir" else installPath
    }

    private fun applyFuelEnvironmentVariables(envVars: EnvVars, appIdInt: Int, productId: String?) {
        val configPath = "C:\\ProgramData"
        envVars.put("FUEL_DIR", "$configPath\\Amazon Games Services\\Legacy")
        envVars.put("AMAZON_GAMES_SDK_PATH", "$configPath\\Amazon Games Services\\AmazonGamesSDK")

        val amazonGame = if (productId != null) {
            runBlocking(Dispatchers.IO) {
                AmazonService.getAmazonGameOf(productId)
            }
        } else {
            null
        }

        if (amazonGame != null) {
            envVars.put("AMAZON_GAMES_FUEL_ENTITLEMENT_ID", amazonGame.entitlementId)
            if (amazonGame.productSku.isNotEmpty()) {
                envVars.put("AMAZON_GAMES_FUEL_PRODUCT_SKU", amazonGame.productSku)
            }
            Timber.tag("XServerScreen").i(
                "FuelPump env: entitlementId=${amazonGame.entitlementId}, sku=${amazonGame.productSku}",
            )
        } else {
            Timber.tag("XServerScreen").w("Could not load AmazonGame for appId=$appIdInt - FuelPump env vars incomplete")
        }

        envVars.put("AMAZON_GAMES_FUEL_DISPLAY_NAME", "Player")
    }

    private fun deployAmazonSdk(context: android.content.Context, container: com.winlator.container.Container) {
        val prefixProgramData = File(container.rootDir, ".wine/drive_c/ProgramData")
        try {
            File(prefixProgramData, "Amazon Games Services/Legacy").mkdirs()
            File(prefixProgramData, "Amazon Games Services/AmazonGamesSDK").mkdirs()

            val sdkToken = runBlocking(Dispatchers.IO) {
                AmazonService.getInstance()?.amazonManager?.getBearerToken()
            }
            if (sdkToken != null) {
                val cached = runBlocking(Dispatchers.IO) {
                    AmazonSdkManager.ensureSdkFiles(context, sdkToken)
                }
                if (cached) {
                    val deployed = AmazonSdkManager.deploySdkToPrefix(context, prefixProgramData)
                    Timber.tag("XServerScreen").i("SDK deployed: $deployed file(s) to Wine prefix")
                } else {
                    Timber.tag("XServerScreen").w("SDK download failed - game may not authenticate (non-fatal)")
                }
            } else {
                Timber.tag("XServerScreen").w("No Amazon bearer token - SDK files not deployed (non-fatal)")
            }
        } catch (e: Exception) {
            Timber.tag("XServerScreen").w(e, "Failed to deploy SDK files (non-fatal)")
        }
    }

    private fun buildAmazonLaunchCommand(amazonCommand: String, fuelArgs: List<String>): String {
        return if (fuelArgs.isNotEmpty()) {
            val argsStr = fuelArgs.joinToString(" ") { arg ->
                if (arg.contains(" ")) "\"$arg\"" else arg
            }
            Timber.tag("XServerScreen").i("Amazon launch command: \"$amazonCommand\" $argsStr")
            "winhandler.exe \"$amazonCommand\" $argsStr"
        } else {
            Timber.tag("XServerScreen").i("Amazon launch command: \"$amazonCommand\"")
            "winhandler.exe \"$amazonCommand\""
        }
    }

    private data class FuelManifest(
        val command: String? = null,
        val workingSubdirOverride: String? = null,
        val args: List<String> = emptyList(),
    )
}
