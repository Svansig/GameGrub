package app.gamegrub.session

import app.gamegrub.cache.CacheController
import app.gamegrub.cache.CacheKeyDerivation
import app.gamegrub.cache.manifest.CacheType
import app.gamegrub.container.store.ContainerStore
import app.gamegrub.runtime.store.RuntimeStore
import app.gamegrub.session.model.BindMount
import app.gamegrub.session.model.CacheHandle
import app.gamegrub.session.model.EnvPlan
import app.gamegrub.session.model.MountPlan
import app.gamegrub.session.model.SessionComposition
import app.gamegrub.session.model.SessionMetadata
import app.gamegrub.session.model.SessionPlan
import app.gamegrub.session.model.SessionState
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Assembles a complete launch session from runtime bundles and container state.
 *
 * Resolves and composes all required components (base, runtime, driver, container)
 * into a SessionPlan that captures all paths, environment variables, and mount
 * mappings needed to launch a game. Uses RuntimeStore and ContainerStore to
 * retrieve or create necessary components.
 *
 * @property runtimeStore Store for runtime bundles
 * @property containerStore Store for container state
 * @property cacheController Controller for cache management
 */
@Singleton
class SessionAssembler @Inject constructor(
    private val runtimeStore: RuntimeStore,
    private val containerStore: ContainerStore,
    private val cacheController: CacheController,
) {

    suspend fun assemble(
        gameId: String,
        gameTitle: String,
        gamePlatform: String,
        baseId: String? = null,
        runtimeId: String? = null,
        driverId: String? = null,
        profileId: String? = null,
        launchArgs: List<String> = emptyList(),
    ): Result<SessionPlan> = withContext(Dispatchers.IO) {
        try {
            val sessionId = UUID.randomUUID().toString()
            Timber.i("Assembling session: $sessionId for $gamePlatform:$gameId")

            val resolvedBaseId = baseId ?: resolveDefaultBaseId()
            val resolvedRuntimeId = runtimeId ?: resolveDefaultRuntimeId()
            val resolvedDriverId = driverId ?: resolveDefaultDriverId()

            val base = runtimeStore.getBase(resolvedBaseId)
            val runtime = runtimeStore.getRuntime(resolvedRuntimeId)
            val driver = resolvedDriverId?.let { runtimeStore.getDriver(it) }

            val container = containerStore.getContainerByGame(gamePlatform, gameId)
                ?: containerStore.createContainer(
                    gameId = gameId,
                    gamePlatform = gamePlatform,
                    baseId = resolvedBaseId,
                    runtimeId = resolvedRuntimeId,
                    driverId = resolvedDriverId,
                    profileId = profileId,
                    name = gameTitle,
                ).getOrThrow()

            val mountPlan = resolveMountPaths(base, runtime, driver, container)
            val envPlan = resolveEnvironmentVariables(base, runtime, driver, container)

            val cacheHandles = resolveCacheHandles(base, runtime, driver, profileId)

            val composition = when {
                base != null && runtime != null -> {
                    SessionComposition.Full(
                        base = base,
                        runtime = runtime,
                        driver = driver,
                        container = container,
                    )
                }

                container != null -> {
                    SessionComposition.Partial(
                        base = base,
                        runtime = runtime,
                        driver = driver,
                        container = container,
                        missingComponents = listOfNotNull(
                            base?.id?.let { null } ?: "base",
                            runtime?.id?.let { null } ?: "runtime",
                        ).filter { it != null }.ifEmpty { listOf("base", "runtime") },
                    )
                }

                else -> {
                    return@withContext Result.failure(IllegalStateException("No container found and could not create one"))
                }
            }

            val sessionPlan = SessionPlan(
                sessionId = sessionId,
                metadata = SessionMetadata(
                    sessionId = sessionId,
                    gameId = gameId,
                    gameTitle = gameTitle,
                    gamePlatform = gamePlatform,
                    launchArgs = launchArgs,
                ),
                state = SessionState.ASSEMBLED,
                composition = composition,
                mountPlan = mountPlan,
                envPlan = envPlan,
                cacheHandles = cacheHandles,
            )

            Timber.i("Session assembled: $sessionId")
            Result.success(sessionPlan)
        } catch (e: Exception) {
            Timber.e(e, "Failed to assemble session for $gamePlatform:$gameId")
            Result.failure(e)
        }
    }

    private fun resolveDefaultBaseId(): String {
        return runtimeStore.listBases().firstOrNull()?.id
            ?: "base-linux-glibc2.35-2.35"
    }

    private fun resolveDefaultRuntimeId(): String {
        return runtimeStore.listRuntimes().firstOrNull()?.id
            ?: "wine-8.0-glibc2.35"
    }

    private fun resolveDefaultDriverId(): String? {
        return runtimeStore.listDrivers().firstOrNull()?.id
    }

    private fun resolveMountPaths(
        base: app.gamegrub.runtime.manifest.BaseManifest?,
        runtime: app.gamegrub.runtime.manifest.RuntimeManifest?,
        driver: app.gamegrub.runtime.manifest.DriverManifest?,
        container: app.gamegrub.container.manifest.ContainerManifest,
    ): MountPlan {
        val basePath = base?.let { runtimeStore.getRootDir().absolutePath + "/runtime-store/bases/${it.id}/rootfs" } ?: ""
        val runtimePath = runtime?.let { runtimeStore.getRootDir().absolutePath + "/runtimes/${it.id}" } ?: ""
        val driverPath = driver?.let { runtimeStore.getRootDir().absolutePath + "/drivers/${it.id}" }

        val containerRoot = containerStore.getRootDir().absolutePath + "/containers/${container.id}"

        return MountPlan(
            baseMount = basePath,
            runtimeMount = runtimePath,
            driverMount = driverPath,
            containerPrefixMount = "$containerRoot/prefix",
            containerInstallMount = "$containerRoot/install",
            containerSavesMount = "$containerRoot/saves",
            containerCacheMount = "$containerRoot/cache",
            tempDirMount = "/tmp/session-${container.id}",
            bindMounts = listOf(
                BindMount(
                    source = "$containerRoot/install",
                    target = "/storage",
                    readOnly = true,
                ),
            ),
        )
    }

    private fun resolveEnvironmentVariables(
        base: app.gamegrub.runtime.manifest.BaseManifest?,
        runtime: app.gamegrub.runtime.manifest.RuntimeManifest?,
        driver: app.gamegrub.runtime.manifest.DriverManifest?,
        container: app.gamegrub.container.manifest.ContainerManifest,
    ): EnvPlan {
        val containerRoot = containerStore.getRootDir().absolutePath + "/containers/${container.id}"

        val envVars = mutableMapOf<String, String>()

        base?.let {
            envVars["GG_BASE_VERSION"] = it.version
        }

        runtime?.let {
            envVars["WINEPREFIX"] = "$containerRoot/prefix"
            envVars["WINEDLLOVERRIDES"] = container.userOverrides["WINEDLLOVERRIDES"] ?: ""
            envVars["WINEESYNC"] = if (container.configuration.csmt) "1" else "0"
        }

        driver?.let {
            envVars["RADV_PERFTEST"] = "gpl"
        }

        val cacheDir = "$containerRoot/cache"

        envVars["DXVK_STATE_CACHE"] = "$cacheDir/shader/dxvk"
        envVars["MESA_SHADER_CACHE_DIR"] = "$cacheDir/shader/mesa"
        envVars["XDG_CACHE_HOME"] = cacheDir

        val pathAdditions = mutableListOf<String>()
        runtime?.let {
            pathAdditions.add(runtimeStore.getRootDir().absolutePath + "/runtimes/${it.id}/bin")
        }

        return EnvPlan(
            environmentVariables = envVars,
            pathAdditions = pathAdditions,
            winePrefix = "$containerRoot/prefix",
            wineRuntime = runtime?.let { runtimeStore.getRootDir().absolutePath + "/runtimes/${it.id}" },
            dxvkStateCache = "$cacheDir/shader/dxvk",
            mesaShaderCache = "$cacheDir/shader/mesa",
            xdgCacheHome = cacheDir,
        )
    }

    private suspend fun resolveCacheHandles(
        base: app.gamegrub.runtime.manifest.BaseManifest?,
        runtime: app.gamegrub.runtime.manifest.RuntimeManifest?,
        driver: app.gamegrub.runtime.manifest.DriverManifest?,
        profileId: String?,
    ): List<CacheHandle> {
        val handles = mutableListOf<CacheHandle>()

        if (base != null && runtime != null) {
            val shaderKey = CacheKeyDerivation.deriveShaderCacheKey(
                baseId = base.id,
                runtimeId = runtime.id,
                driverId = driver?.id ?: "",
            )
            val shaderDir = cacheController.getCacheDir(CacheType.SHADER, shaderKey)
            shaderDir.mkdirs()

            val shaderManifest = cacheController.createCache(
                cacheType = CacheType.SHADER,
                key = shaderKey,
                metadata = mapOf("baseId" to base.id, "runtimeId" to runtime.id, "driverId" to (driver?.id ?: "")),
            ).getOrNull()
            shaderManifest?.let {
                handles.add(CacheHandle.fromManifest(it, shaderDir.absolutePath))
            }

            val translatorKey = profileId?.let {
                CacheKeyDerivation.deriveTranslatorCacheKey(
                    baseId = base.id,
                    runtimeId = runtime.id,
                    profileId = it,
                    exeHash = "",
                )
            }
            translatorKey?.let { key ->
                val translatorDir = cacheController.getCacheDir(CacheType.TRANSLATOR, key)
                translatorDir.mkdirs()
                val translatorManifest = cacheController.createCache(
                    cacheType = CacheType.TRANSLATOR,
                    key = key,
                    metadata = mapOf("baseId" to base.id, "runtimeId" to runtime.id, "profileId" to profileId!!),
                ).getOrNull()
                translatorManifest?.let {
                    handles.add(CacheHandle.fromManifest(it, translatorDir.absolutePath))
                }
            }

            val probeKey = CacheKeyDerivation.deriveProbeCacheKey(
                baseId = base.id,
                runtimeId = runtime.id,
            )
            val probeDir = cacheController.getCacheDir(CacheType.PROBE, probeKey)
            probeDir.mkdirs()
            val probeManifest = cacheController.createCache(
                cacheType = CacheType.PROBE,
                key = probeKey,
                metadata = mapOf("baseId" to base.id, "runtimeId" to runtime.id),
            ).getOrNull()
            probeManifest?.let {
                handles.add(CacheHandle.fromManifest(it, probeDir.absolutePath))
            }
        }

        return handles
    }
}
