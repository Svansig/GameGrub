package app.gamegrub.session.model

import app.gamegrub.cache.manifest.CacheManifest
import app.gamegrub.container.manifest.ContainerManifest
import app.gamegrub.runtime.manifest.BaseManifest
import app.gamegrub.runtime.manifest.DriverManifest
import app.gamegrub.runtime.manifest.RuntimeManifest
import app.gamegrub.telemetry.session.LaunchFingerprint
import kotlinx.serialization.Serializable

/**
 * Complete launch session plan containing all components needed to launch a game.
 *
 * Represents the assembled configuration for a single game launch, including
 * references to runtime bundles, container state, mount mappings, environment
 * variables, and cache handles. Produced by SessionAssembler and consumed
 * by LaunchEngine.
 *
 * @property sessionId Unique identifier for this session
 * @property metadata Session metadata (game info, timestamps)
 * @property state Current state in the session lifecycle
 * @property composition The composed runtime components
 * @property mountPlan Mount point mappings for the session
 * @property envPlan Environment variables for the session
 * @property cacheHandles Cache directories to mount
 * @property fingerprint Launch fingerprint for telemetry
 */
@Serializable
data class SessionPlan(
    val sessionId: String,
    val metadata: SessionMetadata,
    val state: SessionState = SessionState.COMPOSING,
    val composition: SessionComposition,
    val mountPlan: MountPlan,
    val envPlan: EnvPlan,
    val cacheHandles: List<CacheHandle> = emptyList(),
    val fingerprint: LaunchFingerprint? = null,
)

@Serializable
data class SessionMetadata(
    val sessionId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val gameId: String,
    val gameTitle: String,
    val gamePlatform: String,
    val requestedBy: String = "user",
    val launchArgs: List<String> = emptyList(),
)

@Serializable
enum class SessionState {
    COMPOSING,
    ASSEMBLED,
    LAUNCHING,
    LAUNCHED,
    COMPLETED,
    FAILED,
}

@Serializable
sealed class SessionComposition {
    @Serializable
    data class Full(
        val base: BaseManifest,
        val runtime: RuntimeManifest,
        val driver: DriverManifest?,
        val container: ContainerManifest,
    ) : SessionComposition()

    @Serializable
    data class Partial(
        val base: BaseManifest?,
        val runtime: RuntimeManifest?,
        val driver: DriverManifest?,
        val container: ContainerManifest,
        val missingComponents: List<String>,
    ) : SessionComposition()

    @Serializable
    data class Failed(
        val reason: String,
        val missingComponents: List<String>,
    ) : SessionComposition()
}

@Serializable
data class MountPlan(
    val baseMount: String,
    val runtimeMount: String,
    val driverMount: String?,
    val containerPrefixMount: String,
    val containerInstallMount: String,
    val containerSavesMount: String?,
    val containerCacheMount: String,
    val tempDirMount: String,
    val bindMounts: List<BindMount> = emptyList(),
)

@Serializable
data class BindMount(
    val source: String,
    val target: String,
    val readOnly: Boolean = false,
)

@Serializable
data class EnvPlan(
    val environmentVariables: Map<String, String> = emptyMap(),
    val pathAdditions: List<String> = emptyList(),
    val winePrefix: String? = null,
    val wineRuntime: String? = null,
    val dxvkStateCache: String? = null,
    val mesaShaderCache: String? = null,
    val xdgCacheHome: String? = null,
)

@Serializable
data class CacheHandle(
    val cacheId: String,
    val cacheType: String,
    val mountPath: String,
    val key: String,
) {
    companion object {
        fun fromManifest(manifest: app.gamegrub.cache.manifest.CacheManifest, mountPath: String): CacheHandle {
            return CacheHandle(
                cacheId = manifest.id,
                cacheType = manifest.cacheType.name,
                mountPath = mountPath,
                key = manifest.computeCacheKey(),
            )
        }
    }
}