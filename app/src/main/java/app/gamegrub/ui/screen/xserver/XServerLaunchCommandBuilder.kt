package app.gamegrub.ui.screen.xserver

import android.content.Context
import app.gamegrub.container.launch.manager.ContainerLaunchManagerFactory
import app.gamegrub.data.GameSource
import app.gamegrub.data.LaunchInfo
import com.winlator.container.Container
import com.winlator.core.envvars.EnvVars
import com.winlator.xenvironment.components.GuestProgramLauncherComponent

/**
 * Backward-compatible facade used by `XServerScreen` while launch logic is
 * migrated into container launch command strategies.
 */
internal object XServerLaunchCommandBuilder {
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
    ): String {
        val launchManager = ContainerLaunchManagerFactory.create()
        return launchManager.buildWineStartCommand(
            context = context,
            appId = appId,
            container = container,
            bootToContainer = bootToContainer,
            testGraphics = testGraphics,
            appLaunchInfo = appLaunchInfo,
            envVars = envVars,
            guestProgramLauncherComponent = guestProgramLauncherComponent,
            gameSource = gameSource,
        )
    }
}
