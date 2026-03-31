package app.gamegrub.container.launch.command

import app.gamegrub.data.GameSource
import app.gamegrub.utils.game.CustomGameScanner
import java.io.File
import timber.log.Timber

/**
 * Handles custom game launches that rely on A: drive mapping and executable autodetection.
 */
internal object CustomGameLaunchCommandBuilder : StoreLaunchCommandBuilder {
    override fun build(context: LaunchCommandContext): String? {
        if (context.gameSource != GameSource.CUSTOM_GAME) {
            return null
        }

        var executablePath = context.container.executablePath
        val gameFolderPath = findADrivePath(context.container)

        if (executablePath.isEmpty()) {
            if (gameFolderPath == null) {
                Timber.tag("XServerScreen").e("Could not find A: drive for Custom Game: ${context.appId}")
                return wrapWithWinhandler("\"wfm.exe\"")
            }

            val auto = CustomGameScanner.get().findUniqueExeRelativeToFolder(gameFolderPath)
            if (auto != null) {
                Timber.tag("XServerScreen").i("Auto-selected Custom Game exe: $auto")
                executablePath = auto
                context.container.executablePath = auto
                context.container.saveData()
            } else {
                Timber.tag("XServerScreen").w("No unique executable found for Custom Game: ${context.appId}")
                return wrapWithWinhandler("\"wfm.exe\"")
            }
        }

        if (gameFolderPath == null) {
            Timber.tag("XServerScreen").e("Could not find A: drive for Custom Game: ${context.appId}")
            return wrapWithWinhandler("\"wfm.exe\"")
        }

        val executableDir = gameFolderPath + "/" + executablePath.substringBeforeLast("/", "")
        context.guestProgramLauncherComponent.workingDir = File(executableDir)

        val normalizedPath = executablePath.replace('/', '\\')
        context.envVars.put("WINEPATH", "A:\\")
        return wrapWithWinhandler("\"A:\\${normalizedPath}\"")
    }

    private fun findADrivePath(container: com.winlator.container.Container): String? {
        for (drive in com.winlator.container.Container.drivesIterator(container.drives)) {
            if (drive[0] == "A") {
                return drive[1]
            }
        }
        return null
    }

    private fun wrapWithWinhandler(args: String): String {
        return "winhandler.exe $args"
    }
}
