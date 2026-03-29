package app.gamegrub.container.launch.command

import app.gamegrub.service.steam.SteamService
import app.gamegrub.utils.steam.SteamUtils
import java.io.File
import timber.log.Timber

/**
 * Handles Steam-specific prelaunch side effects and command generation.
 */
internal object SteamLaunchCommandBuilder : StoreLaunchCommandBuilder {
    fun prepare(context: LaunchCommandContext) {
        if (context.container.executablePath.isEmpty()) {
            context.container.executablePath = SteamService.getInstalledExe(context.gameId)
            context.container.saveData()
        }

        if (!context.container.isUseLegacyDRM) {
            SteamUtils.writeColdClientIni(context.gameId, context.container)
        }

        val controllerVdfText = SteamService.resolveSteamControllerVdfText(context.gameId)
        if (controllerVdfText.isNullOrEmpty()) {
            Timber.tag("XServerScreen").i("No steam controller VDF resolved for ${context.gameId}")
        } else {
            Timber.tag("XServerScreen").i("Resolved steam controller VDF for ${context.gameId}")
        }
    }

    override fun build(context: LaunchCommandContext): String? {
        if (context.gameSource != app.gamegrub.data.GameSource.STEAM) {
            return null
        }

        return if (context.appLaunchInfo == null) {
            Timber.tag("XServerScreen").w("appLaunchInfo is null for Steam game: ${context.appId}")
            wrapWithWinhandler("\"wfm.exe\"")
        } else {
            wrapWithWinhandler(buildSteamArgs(context))
        }
    }

    private fun buildSteamArgs(context: LaunchCommandContext): String {
        if (context.container.isLaunchRealSteam) {
            return "\"C:\\\\Program Files (x86)\\\\Steam\\\\steam.exe\" -silent -vgui -tcp " +
                "-nobigpicture -nofriendsui -nochatui -nointro -applaunch ${context.gameId}"
        }

        val executablePath = if (context.container.executablePath.isNotEmpty()) {
            context.container.executablePath
        } else {
            val detected = SteamService.getInstalledExe(context.gameId)
            context.container.executablePath = detected
            context.container.saveData()
            detected
        }

        if (!context.container.isUseLegacyDRM) {
            return "\"C:\\\\Program Files (x86)\\\\Steam\\\\steamclient_loader_x64.exe\""
        }

        val appDirPath = SteamService.getAppDirPath(context.gameId)
        val executableDir = appDirPath + "/" + executablePath.substringBeforeLast("/", "")
        context.guestProgramLauncherComponent.workingDir = File(executableDir)
        Timber.i("Working directory is $executableDir")
        Timber.i("Final exe path is $executablePath")

        val drives = context.container.drives
        val driveIndex = drives.indexOf(appDirPath)
        val drive = if (driveIndex > 1) {
            drives[driveIndex - 2]
        } else {
            Timber.e("Could not locate game drive")
            'D'
        }
        context.envVars.put("WINEPATH", "$drive:/${context.appLaunchInfo!!.workingDir}")
        return "\"$drive:/$executablePath\""
    }

    private fun wrapWithWinhandler(args: String): String {
        return "winhandler.exe $args"
    }
}

