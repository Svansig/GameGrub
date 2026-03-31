package app.gamegrub.container.launch.prep

import android.content.Context
import app.gamegrub.container.manager.ContainerRuntimeManager
import app.gamegrub.ui.data.XServerState
import com.winlator.container.Container
import com.winlator.contents.ContentsManager
import com.winlator.core.OnExtractFileListener
import com.winlator.core.WineRegistryEditor
import com.winlator.core.envvars.EnvVars
import com.winlator.xenvironment.ImageFs
import com.winlator.xserver.ScreenInfo
import java.io.File

/**
 * Groups launch preparation steps that run before XEnvironment creation.
 *
 * This keeps XServerScreen orchestration thin while preserving existing order.
 */
internal object LaunchPreparationCoordinator {
    fun prepareLaunchArtifacts(
        context: Context,
        firstTimeBoot: Boolean,
        screenInfo: ScreenInfo,
        xServerState: androidx.compose.runtime.MutableState<XServerState>,
        container: Container,
        containerManager: ContainerRuntimeManager,
        envVars: EnvVars,
        contentsManager: ContentsManager,
        onExtractFileListener: OnExtractFileListener?,
        vkbasaltConfig: String,
        alwaysReextract: Boolean,
    ) {
        WineSystemFilesCoordinator.setupWineSystemFiles(
            context = context,
            firstTimeBoot = firstTimeBoot,
            screenInfo = screenInfo,
            xServerState = xServerState,
            container = container,
            containerManager = containerManager,
            envVars = envVars,
            contentsManager = contentsManager,
            onExtractFileListener = onExtractFileListener,
            alwaysReextract = alwaysReextract,
        )

        InputDllPreparationCoordinator.extractArm64ecInputDlls(context, container)
        InputDllPreparationCoordinator.extractX8664InputDlls(context, container)

        GraphicsDriverPreparationCoordinator.extractGraphicsDriverFiles(
            context = context,
            graphicsDriver = xServerState.value.graphicsDriver,
            dxwrapper = xServerState.value.dxwrapper,
            dxwrapperConfig = xServerState.value.dxwrapperConfig!!,
            container = container,
            envVars = envVars,
            firstTimeBoot = firstTimeBoot,
            vkbasaltConfig = vkbasaltConfig,
            alwaysReextract = alwaysReextract,
        )

        changeWineAudioDriver(context, xServerState.value.audioDriver, container)
        setImagefsContainerVariant(context, container)
    }

    private fun changeWineAudioDriver(context: Context, audioDriver: String, container: Container) {
        if (audioDriver != container.getExtra("audioDriver")) {
            val imageFs = ImageFs.find(context)
            val rootDir = imageFs.rootDir
            val userRegFile = File(rootDir, ImageFs.WINEPREFIX + "/user.reg")
            WineRegistryEditor(userRegFile).use { registryEditor ->
                if (audioDriver == "alsa") {
                    registryEditor.setStringValue("Software\\Wine\\Drivers", "Audio", "alsa")
                } else if (audioDriver == "pulseaudio") {
                    registryEditor.setStringValue("Software\\Wine\\Drivers", "Audio", "pulse")
                }
            }
            container.putExtra("audioDriver", audioDriver)
            container.saveData()
        }
    }

    private fun setImagefsContainerVariant(context: Context, container: Container) {
        val imageFs = ImageFs.find(context)
        imageFs.createVariantFile(container.containerVariant)
        imageFs.createArchFile(container.wineVersion)
    }
}
