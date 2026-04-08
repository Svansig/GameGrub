package app.gamegrub.graphics.cache

import java.io.File

/**
 * Capabilities for graphics cache types.
 *
 * Identifies which graphics APIs and cache systems are supported
 * by a given adapter implementation.
 */
enum class CacheCapability {
    DXVK_STATE_CACHE,
    VKD3D_SHADER_CACHE,
    MESA_SHADER_CACHE,
    GENERIC,
}

/**
 * Configuration for graphics cache paths and options.
 *
 * @property dxvkStateCachePath Path to DXVK state cache directory
 * @property vkd3dShaderCachePath Path to VKD3D shader cache directory
 * @property mesaShaderCachePath Path to Mesa shader cache directory
 * @property xdgCacheHome XDG-compliant cache home directory
 * @property enableStateCache Whether to enable DXVK/VKD3D state cache
 * @property enableAsyncCompute Whether to enable async compute for shaders
 */
data class CacheConfiguration(
    val dxvkStateCachePath: String? = null,
    val vkd3dShaderCachePath: String? = null,
    val mesaShaderCachePath: String? = null,
    val xdgCacheHome: String? = null,
    val enableStateCache: Boolean = true,
    val enableAsyncCompute: Boolean = false,
) {
    fun isEnabled(): Boolean = dxvkStateCachePath != null || vkd3dShaderCachePath != null || mesaShaderCachePath != null
}

/**
 * Adapter for managing graphics API-specific caches.
 *
 * Provides a common interface for configuring and managing caches for
 * different graphics APIs including DXVK, VKD3D, and Mesa. Each adapter
 * handles the specific environment variables and cache directories
 * for its respective graphics API.
 *
 * @property capability The cache capability this adapter supports
 * @property name Human-readable name for this adapter
 */
interface GraphicsCacheAdapter {
    val capability: CacheCapability
    val name: String

    fun isSupported(): Boolean

    fun setup(cacheDir: File): CacheConfiguration

    fun configure(envVars: MutableMap<String, String>): CacheConfiguration

    fun cleanup(cacheDir: File)

    fun getCapabilityRequirements(): Set<CacheCapability> = setOf(capability)

    companion object {
        fun createAdapter(capability: CacheCapability): GraphicsCacheAdapter {
            return when (capability) {
                CacheCapability.DXVK_STATE_CACHE -> DxvkCacheAdapter()
                CacheCapability.VKD3D_SHADER_CACHE -> Vkd3dCacheAdapter()
                CacheCapability.MESA_SHADER_CACHE -> MesaCacheAdapter()
                CacheCapability.GENERIC -> GenericCacheAdapter()
            }
        }

        fun createAllAdapters(): List<GraphicsCacheAdapter> {
            return listOf(
                DxvkCacheAdapter(),
                Vkd3dCacheAdapter(),
                MesaCacheAdapter(),
                GenericCacheAdapter(),
            )
        }
    }
}

class DxvkCacheAdapter : GraphicsCacheAdapter {
    override val capability = CacheCapability.DXVK_STATE_CACHE
    override val name = "DXVK"

    override fun isSupported(): Boolean = true

    override fun setup(cacheDir: File): CacheConfiguration {
        val dxvkDir = File(cacheDir, "dxvk")
        dxvkDir.mkdirs()
        return CacheConfiguration(
            dxvkStateCachePath = dxvkDir.absolutePath,
            enableStateCache = true,
        )
    }

    override fun configure(envVars: MutableMap<String, String>): CacheConfiguration {
        val dxvkPath = envVars["DXVK_STATE_CACHE"] ?: return CacheConfiguration()

        val config = CacheConfiguration(
            dxvkStateCachePath = dxvkPath,
            enableStateCache = true,
        )

        if (config.enableStateCache) {
            envVars["DXVK_ASYNC"] = "1"
            envVars["DXVK_STATE_CACHE"] = dxvkPath
        }

        return config
    }

    override fun cleanup(cacheDir: File) {
        val dxvkDir = File(cacheDir, "dxvk")
        if (dxvkDir.exists()) {
            dxvkDir.deleteRecursively()
        }
    }
}

class Vkd3dCacheAdapter : GraphicsCacheAdapter {
    override val capability = CacheCapability.VKD3D_SHADER_CACHE
    override val name = "VKD3D"

    override fun isSupported(): Boolean = true

    override fun setup(cacheDir: File): CacheConfiguration {
        val vkd3dDir = File(cacheDir, "vkd3d")
        vkd3dDir.mkdirs()
        return CacheConfiguration(
            vkd3dShaderCachePath = vkd3dDir.absolutePath,
        )
    }

    override fun configure(envVars: MutableMap<String, String>): CacheConfiguration {
        val vkd3dPath = envVars["VKD3D_SHADER_CACHE"] ?: return CacheConfiguration()

        val config = CacheConfiguration(
            vkd3dShaderCachePath = vkd3dPath,
        )

        envVars["VKD3D_SHADER_CACHE"] = vkd3dPath
        envVars["VKD3D_STATE_CACHE"] = vkd3dPath

        return config
    }

    override fun cleanup(cacheDir: File) {
        val vkd3dDir = File(cacheDir, "vkd3d")
        if (vkd3dDir.exists()) {
            vkd3dDir.deleteRecursively()
        }
    }
}

class MesaCacheAdapter : GraphicsCacheAdapter {
    override val capability = CacheCapability.MESA_SHADER_CACHE
    override val name = "Mesa"

    override fun isSupported(): Boolean {
        return System.getProperty("os.name").lowercase().contains("linux")
    }

    override fun setup(cacheDir: File): CacheConfiguration {
        val mesaDir = File(cacheDir, "mesa")
        mesaDir.mkdirs()
        return CacheConfiguration(
            mesaShaderCachePath = mesaDir.absolutePath,
            xdgCacheHome = cacheDir.parentFile?.absolutePath,
        )
    }

    override fun configure(envVars: MutableMap<String, String>): CacheConfiguration {
        val mesaCache = envVars["MESA_SHADER_CACHE_DIR"]
        val xdgCache = envVars["XDG_CACHE_HOME"]

        val config = CacheConfiguration(
            mesaShaderCachePath = mesaCache,
            xdgCacheHome = xdgCache,
        )

        if (mesaCache != null) {
            envVars["MESA_SHADER_CACHE_DIR"] = mesaCache
            envVars["RADV_PERFTEST"] = "rt"
        }

        if (xdgCache != null) {
            envVars["XDG_CACHE_HOME"] = xdgCache
        }

        return config
    }

    override fun cleanup(cacheDir: File) {
        val mesaDir = File(cacheDir, "mesa")
        if (mesaDir.exists()) {
            mesaDir.deleteRecursively()
        }
    }
}

class GenericCacheAdapter : GraphicsCacheAdapter {
    override val capability = CacheCapability.GENERIC
    override val name = "Generic"

    override fun isSupported(): Boolean = true

    override fun setup(cacheDir: File): CacheConfiguration {
        return CacheConfiguration(
            xdgCacheHome = cacheDir.absolutePath,
        )
    }

    override fun configure(envVars: MutableMap<String, String>): CacheConfiguration {
        val xdgCache = envVars["XDG_CACHE_HOME"]
        return CacheConfiguration(xdgCacheHome = xdgCache)
    }

    override fun cleanup(cacheDir: File) {
    }
}
