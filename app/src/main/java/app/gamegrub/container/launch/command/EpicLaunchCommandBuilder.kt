package app.gamegrub.container.launch.command

import app.gamegrub.data.GameSource
import app.gamegrub.service.epic.EpicService
import java.io.File
import kotlinx.coroutines.runBlocking
import timber.log.Timber

/**
 * Builds Epic launch commands and applies Epic-provided auth arguments.
 */
internal object EpicLaunchCommandBuilder : BaseLaunchCommandBuilder() {
    override val gameSource: GameSource = GameSource.EPIC

    override fun buildStoreCommand(context: LaunchCommandContext): String? {
        Timber.tag("XServerScreen").i("Launching Epic game: ${context.gameId}")
        val game = runBlocking {
            EpicService.getInstance()?.epicManager?.getGameById(context.gameId)
        }

        if (game == null || !game.isInstalled || game.installPath.isEmpty()) {
            Timber.tag("XServerScreen").e("Cannot launch: Epic game not installed")
            return "\"explorer.exe\""
        }

        val exePath = context.container.executablePath.ifEmpty {
            val detectedPath = runBlocking {
                EpicService.getInstance()?.epicManager?.getInstalledExe(game.id) ?: ""
            }
            if (detectedPath.isNotEmpty()) {
                context.container.executablePath = detectedPath
                context.container.saveData()
            }
            detectedPath
        }

        if (exePath.isEmpty()) {
            Timber.tag("XServerScreen").e("Cannot launch: executable not found for Epic game")
            return "\"explorer.exe\""
        }

        val relativePath = exePath.removePrefix(game.installPath).removePrefix("/")
        val epicCommand = "A:\\$relativePath".replace("/", "\\")

        Timber.tag("XServerScreen").d("Building Epic launch parameters for ${game.appName}...")
        val runArguments: List<String> = runBlocking {
            val result = EpicService.buildLaunchParameters(context.context, game, false)
            if (result.isFailure) {
                Timber.tag("XServerScreen").e(result.exceptionOrNull(), "Failed to build Epic launch parameters")
            }
            val params = result.getOrNull() ?: listOf()
            Timber.tag("XServerScreen").i("Got ${params.size} Epic launch parameters")
            params
        }

        val executableDir = game.installPath + "/" + relativePath.substringBeforeLast("/", "")
        context.guestProgramLauncherComponent.workingDir = File(executableDir)

        val launchCommand = if (runArguments.isNotEmpty()) {
            val launchArgs = runArguments.joinToString(" ") { arg ->
                if (arg.contains("=") && arg.substringAfter("=").contains(" ")) {
                    val (key, value) = arg.split("=", limit = 2)
                    "$key=\"$value\""
                } else if (arg.contains(" ")) {
                    "\"$arg\""
                } else {
                    arg
                }
            }
            "winhandler.exe \"$epicCommand\" $launchArgs"
        } else {
            Timber.tag("XServerScreen").w("No Epic launch parameters available, launching without authentication")
            "winhandler.exe \"$epicCommand\""
        }

        val redactedCommand = launchCommand
            .replace(Regex("-AUTH_PASSWORD=(\"[^\"]*\"|[^ ]+)"), "-AUTH_PASSWORD=[REDACTED]")
            .replace(Regex("-epicovt=(\"[^\"]*\"|[^ ]+)"), "-epicovt=[REDACTED]")
        Timber.tag("XServerScreen").i("Epic launch command: $redactedCommand")

        return launchCommand
    }
}
