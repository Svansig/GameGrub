package app.gamegrub.container.launch.command

import app.gamegrub.data.GameSource
import com.winlator.core.FileUtils
import java.io.File
import timber.log.Timber

/**
 * Resolves the final Wine launch command by combining shared prechecks and
 * store-specific command builders.
 */
internal object StoreLaunchCommandResolver {
    private val storeBuilders: List<StoreLaunchCommandBuilder> = listOf(
        GogLaunchCommandBuilder,
        EpicLaunchCommandBuilder,
        AmazonLaunchCommandBuilder,
        CustomGameLaunchCommandBuilder,
        SteamLaunchCommandBuilder,
    )

    fun build(context: LaunchCommandContext): String {
        clearWineTempDirectory(context)
        Timber.tag("XServerScreen").d("appLaunchInfo is ${context.appLaunchInfo}")

        if (context.gameSource == GameSource.STEAM) {
            SteamLaunchCommandBuilder.prepare(context)
        }

        if (context.testGraphics) {
            return wrapWithWinhandler("\"Z:/opt/apps/TestD3D.exe\"")
        }

        if (context.bootToContainer) {
            return wrapWithWinhandler("\"wfm.exe\"")
        }

        val storeCommand = storeBuilders.firstNotNullOfOrNull { builder ->
            builder.build(context)
        }

        if (storeCommand != null) {
            return storeCommand
        }

        Timber.tag("XServerScreen").w("Unhandled game source ${context.gameSource}, falling back to container desktop")
        return wrapWithWinhandler("\"wfm.exe\"")
    }

    private fun clearWineTempDirectory(context: LaunchCommandContext) {
        val tempDir = File(context.container.rootDir, ".wine/drive_c/windows/temp")
        FileUtils.clear(tempDir)
    }

    private fun wrapWithWinhandler(args: String): String {
        return "winhandler.exe $args"
    }
}
