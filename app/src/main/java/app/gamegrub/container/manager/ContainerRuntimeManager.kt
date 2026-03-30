package app.gamegrub.container.manager

import com.winlator.container.Container
import com.winlator.contents.ContentsManager
import com.winlator.core.OnExtractFileListener
import java.io.File

/**
 * Narrow runtime-facing container API used by XServer launch flows.
 *
 * This wrapper isolates UI and orchestration code from direct dependency on
 * Winlator's ContainerManager while we incrementally migrate launch logic.
 */
internal interface ContainerRuntimeManager {
    /**
     * Makes the target container the active runtime container.
     */
    fun activate(container: Container)

    /**
     * Extracts container pattern files for a Wine version into a container root.
     */
    fun extractPattern(
        wineVersion: String,
        contentsManager: ContentsManager,
        containerRootDir: File,
        onExtractFileListener: OnExtractFileListener?,
    )
}
