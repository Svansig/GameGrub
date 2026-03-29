package app.gamegrub.container.launch.manager

import android.content.Context
import app.gamegrub.data.GameSource
import app.gamegrub.data.LaunchInfo
import com.winlator.container.Container
import com.winlator.core.envvars.EnvVars
import com.winlator.xenvironment.components.GuestProgramLauncherComponent

/**
 * Container-domain entrypoint for generating the guest launch command.
 *
 * Implementations are allowed to mutate container metadata, env vars, and
 * guest launcher working directory to preserve current launch behavior.
 */
internal interface ContainerLaunchManager {
    fun buildWineStartCommand(
        context: Context,
        appId: String,
        container: Container,
        bootToContainer: Boolean,
        testGraphics: Boolean,
        appLaunchInfo: LaunchInfo?,
        envVars: EnvVars,
        guestProgramLauncherComponent: GuestProgramLauncherComponent,
        gameSource: GameSource,
    ): String
}

