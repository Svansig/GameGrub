package app.gamegrub.container.manager

import android.content.Context
import com.winlator.container.ContainerManager

/**
 * Temporary factory used before this runtime manager is provided via DI.
 */
internal object ContainerRuntimeManagerFactory {
    fun create(context: Context): ContainerRuntimeManager {
        return WinlatorContainerRuntimeManager(ContainerManager(context))
    }
}

