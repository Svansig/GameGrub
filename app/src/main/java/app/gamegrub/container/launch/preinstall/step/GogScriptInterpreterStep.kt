package app.gamegrub.container.launch.preinstall.step

import app.gamegrub.data.GameSource
import app.gamegrub.enums.Marker
import app.gamegrub.service.gog.GOGService
import app.gamegrub.storage.StorageManager
import com.winlator.container.Container
import java.io.File

object GogScriptInterpreterStep : PreInstallStep {
    override val marker: Marker = Marker.GOG_SCRIPT_INSTALLED

    override fun appliesTo(
        container: Container,
        gameSource: GameSource,
        gameDirPath: String,
    ): Boolean {
        return gameSource == GameSource.GOG &&
                container.containerVariant.equals(Container.GLIBC) &&
                !StorageManager.hasMarker(gameDirPath, Marker.GOG_SCRIPT_INSTALLED)
    }

    override fun buildCommand(
        container: Container,
        appId: String,
        gameSource: GameSource,
        gameDir: File,
        gameDirPath: String,
    ): String? {
        val parts = GOGService.getInstance()?.gogManager
            ?.getScriptInterpreterPartsForLaunchSync(appId) ?: return null
        return if (parts.isEmpty()) null else parts.joinToString(" & ")
    }
}
