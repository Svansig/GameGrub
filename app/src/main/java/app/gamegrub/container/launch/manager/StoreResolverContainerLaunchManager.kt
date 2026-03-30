package app.gamegrub.container.launch.manager

import android.content.Context
import app.gamegrub.container.launch.command.LaunchCommandContext
import app.gamegrub.container.launch.command.StoreLaunchCommandResolver
import app.gamegrub.data.GameSource
import app.gamegrub.data.LaunchInfo
import app.gamegrub.utils.container.ContainerUtils
import com.winlator.container.Container
import com.winlator.core.envvars.EnvVars
import com.winlator.xenvironment.components.GuestProgramLauncherComponent

/**
 * Default launch manager implementation backed by store-specific command
 * strategies resolved by [StoreLaunchCommandResolver].
 */
internal class StoreResolverContainerLaunchManager : ContainerLaunchManager {
    override fun buildWineStartCommand(
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
        val launchContext = LaunchCommandContext(
            context = context,
            appId = appId,
            gameId = ContainerUtils.extractGameIdFromContainerId(appId),
            container = container,
            bootToContainer = bootToContainer,
            testGraphics = testGraphics,
            appLaunchInfo = appLaunchInfo,
            envVars = envVars,
            guestProgramLauncherComponent = guestProgramLauncherComponent,
            gameSource = gameSource,
        )
        return StoreLaunchCommandResolver.build(launchContext)
    }
}
