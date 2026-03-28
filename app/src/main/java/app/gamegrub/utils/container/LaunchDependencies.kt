package app.gamegrub.utils.container

import android.content.Context
import app.gamegrub.R
import app.gamegrub.data.GameSource
import app.gamegrub.utils.launchdependencies.GogScriptInterpreterDependency
import app.gamegrub.utils.launchdependencies.LaunchDependency
import app.gamegrub.utils.launchdependencies.LaunchDependencyCallbacks
import com.winlator.container.Container

const val LOADING_PROGRESS_UNKNOWN: Float = -1f

/**
 * Ensures all dependencies required to launch a container are downloaded and installed.
 * Reports progress via the given callbacks.
 * [gameSource] and [gameId] are extracted once by the caller (e.g. GameGrubMain) and passed down.
 */
class LaunchDependencies {
    companion object {
        private val launchDependencies: List<LaunchDependency> = listOf(
            GogScriptInterpreterDependency,
        )
    }

    fun getLaunchDependencies(container: Container, gameSource: GameSource, gameId: Int): List<LaunchDependency> =
        launchDependencies.filter { it.appliesTo(container, gameSource, gameId) }

    suspend fun ensureLaunchDependencies(
        context: Context,
        container: Container,
        gameSource: GameSource,
        gameId: Int,
        setLoadingMessage: (String) -> Unit,
        setLoadingProgress: (Float) -> Unit,
    ) {
        val callbacks = LaunchDependencyCallbacks(setLoadingMessage, setLoadingProgress)
        try {
            for (dep in getLaunchDependencies(container, gameSource, gameId)) {
                if (!dep.isSatisfied(context, container, gameSource, gameId)) {
                    setLoadingMessage(dep.getLoadingMessage(context, container, gameSource, gameId))
                    dep.install(context, container, callbacks, gameSource, gameId)
                }
            }
        } finally {
            setLoadingMessage(context.getString(R.string.main_loading))
            setLoadingProgress(1f)
        }
    }
}
