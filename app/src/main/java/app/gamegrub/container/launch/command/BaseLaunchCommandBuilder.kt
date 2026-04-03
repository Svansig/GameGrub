package app.gamegrub.container.launch.command

import app.gamegrub.data.GameSource
import com.winlator.core.envvars.EnvVars
import com.winlator.xenvironment.components.GuestProgramLauncherComponent

abstract class BaseLaunchCommandBuilder : StoreLaunchCommandBuilder {

    protected abstract val gameSource: GameSource

    override fun build(context: LaunchCommandContext): String? {
        if (context.gameSource != gameSource) {
            return null
        }
        return buildStoreCommand(context)
    }

    protected abstract fun buildStoreCommand(context: LaunchCommandContext): String?

    protected fun wrapWithWinhandler(args: String): String {
        return "winhandler.exe $args"
    }

    protected fun setupWorkingDirectory(context: LaunchCommandContext, dirPath: String) {
        context.guestProgramLauncherComponent.workingDir = java.io.File(dirPath)
    }

    protected fun setupEnvVars(context: LaunchCommandContext, key: String, value: String) {
        context.envVars.put(key, value)
    }

    protected fun setExecutablePath(context: LaunchCommandContext, path: String) {
        context.container.executablePath = path
        context.container.saveData()
    }
}
