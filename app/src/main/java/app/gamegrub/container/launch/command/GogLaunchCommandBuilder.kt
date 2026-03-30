package app.gamegrub.container.launch.command

import app.gamegrub.data.GameSource
import app.gamegrub.data.LibraryItem
import app.gamegrub.service.gog.GOGService
import timber.log.Timber

/**
 * Builds the command used to launch a GOG game through the existing GOG service.
 */
internal object GogLaunchCommandBuilder : StoreLaunchCommandBuilder {
    override fun build(context: LaunchCommandContext): String? {
        if (context.gameSource != GameSource.GOG) {
            return null
        }

        Timber.tag("XServerScreen").i("Launching GOG game: ${context.gameId}")

        val libraryItem = LibraryItem(
            appId = context.appId,
            name = "",
            gameSource = GameSource.GOG,
        )

        val gogCommand = GOGService.getGogWineStartCommandSync(
            libraryItem = libraryItem,
            container = context.container,
            bootToContainer = context.bootToContainer,
            appLaunchInfo = context.appLaunchInfo,
            envVars = context.envVars,
            guestProgramLauncherComponent = context.guestProgramLauncherComponent,
            gameId = context.gameId,
        )

        Timber.tag("XServerScreen").i("GOG launch command: $gogCommand")
        return "winhandler.exe $gogCommand"
    }
}
