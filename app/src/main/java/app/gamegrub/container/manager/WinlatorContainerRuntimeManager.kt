package app.gamegrub.container.manager

import com.winlator.container.Container
import com.winlator.container.ContainerManager
import com.winlator.contents.ContentsManager
import com.winlator.core.OnExtractFileListener
import java.io.File

/**
 * Adapter that forwards runtime container operations to Winlator ContainerManager.
 */
internal class WinlatorContainerRuntimeManager(
    private val delegate: ContainerManager,
) : ContainerRuntimeManager {
    override fun activate(container: Container) {
        delegate.activateContainer(container)
    }

    override fun extractPattern(
        wineVersion: String,
        contentsManager: ContentsManager,
        containerRootDir: File,
        onExtractFileListener: OnExtractFileListener?,
    ) {
        delegate.extractContainerPatternFile(
            wineVersion,
            contentsManager,
            containerRootDir,
            onExtractFileListener,
        )
    }
}
