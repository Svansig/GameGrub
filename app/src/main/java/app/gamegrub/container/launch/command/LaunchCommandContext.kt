package app.gamegrub.container.launch.command

import android.content.Context
import app.gamegrub.data.GameSource
import app.gamegrub.data.LaunchInfo
import com.winlator.container.Container
import com.winlator.core.envvars.EnvVars
import com.winlator.xenvironment.components.GuestProgramLauncherComponent

/**
 * Immutable input for store-specific launch command resolution.
 *
 * Builders can mutate [container], [envVars], and [guestProgramLauncherComponent]
 * exactly like the original monolithic launch method did.
 */
internal data class LaunchCommandContext(
    val context: Context,
    val appId: String,
    val gameId: Int,
    val container: Container,
    val bootToContainer: Boolean,
    val testGraphics: Boolean,
    val appLaunchInfo: LaunchInfo?,
    val envVars: EnvVars,
    val guestProgramLauncherComponent: GuestProgramLauncherComponent,
    val gameSource: GameSource,
)

