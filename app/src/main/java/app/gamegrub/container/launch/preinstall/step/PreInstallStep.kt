package app.gamegrub.container.launch.preinstall.step

import app.gamegrub.data.GameSource
import app.gamegrub.enums.Marker
import com.winlator.container.Container
import java.io.File

interface PreInstallStep {
    val marker: Marker

    fun appliesTo(
        container: Container,
        gameSource: GameSource,
        gameDirPath: String,
    ): Boolean

    fun buildCommand(
        container: Container,
        appId: String,
        gameSource: GameSource,
        gameDir: File,
        gameDirPath: String,
    ): String?
}
