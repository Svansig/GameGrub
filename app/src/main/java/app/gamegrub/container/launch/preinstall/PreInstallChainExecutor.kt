package app.gamegrub.container.launch.preinstall

import app.gamegrub.events.AndroidEvent
import app.gamegrub.ui.runtime.XServerRuntime
import app.gamegrub.ui.screen.xserver.XServerExitCoordinator
import com.winlator.container.Container
import com.winlator.core.Callback
import com.winlator.xenvironment.components.GuestProgramLauncherComponent
import timber.log.Timber

internal fun shouldReportGuestTerminationAsError(status: Int, isExitInProgress: Boolean): Boolean {
    return status != 0 && !isExitInProgress
}

/**
 * Configures the guest launcher termination flow for optional pre-install steps.
 *
 * Behavior is kept equivalent to the old inline setup code:
 * - run each pre-install command in sequence,
 * - mark each step done,
 * - restart the launcher between steps,
 * - then switch back to the real game executable.
 */
internal object PreInstallChainExecutor {
    fun configure(
        container: Container,
        preInstallCommands: List<PreInstallSteps.PreInstallCommand>,
        gameExecutable: String,
        guestProgramLauncherComponent: GuestProgramLauncherComponent,
        onGameLaunchError: ((String) -> Unit)? = null,
    ) {
        val gameTerminationCallback = Callback<Int> { status ->
            if (shouldReportGuestTerminationAsError(status, XServerExitCoordinator.isExitInProgress())) {
                Timber.e("Guest program terminated with status: $status")
                onGameLaunchError?.invoke("Game terminated with error status: $status")
            } else {
                Timber.i("Guest program terminated with status: $status")
            }
            XServerRuntime.get().events.emit(AndroidEvent.GuestProgramTerminated)
        }

        fun chainPreInstallSteps(remaining: List<PreInstallSteps.PreInstallCommand>) {
            if (remaining.isEmpty()) {
                guestProgramLauncherComponent.guestExecutable = gameExecutable
                guestProgramLauncherComponent.terminationCallback = gameTerminationCallback
                return
            }

            guestProgramLauncherComponent.guestExecutable = remaining.first().executable
            guestProgramLauncherComponent.setTerminationCallback { _ ->
                val current = remaining.first()
                PreInstallSteps.markStepDone(container, current.marker)
                guestProgramLauncherComponent.setPreUnpack(null)
                try {
                    guestProgramLauncherComponent.execShellCommand("wineserver -k")
                } catch (e: Exception) {
                    Timber.w(e, "wineserver -k between pre-install steps (non-fatal)")
                }

                val nextRemaining = remaining.drop(1)
                if (nextRemaining.isEmpty()) {
                    XServerRuntime.get().events.emit(AndroidEvent.SetBootingSplashText("Launching game..."))
                } else {
                    XServerRuntime.get().events.emit(AndroidEvent.SetBootingSplashText("Installing prerequisites..."))
                }
                chainPreInstallSteps(nextRemaining)
                guestProgramLauncherComponent.start()
            }
        }

        if (preInstallCommands.isNotEmpty()) {
            chainPreInstallSteps(preInstallCommands)
        } else {
            guestProgramLauncherComponent.terminationCallback = gameTerminationCallback
        }
    }
}
