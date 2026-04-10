package app.gamegrub.graphics.diagnostics

import android.opengl.EGL14
import android.opengl.EGLConfig
import app.gamegrub.BuildConfig
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import timber.log.Timber

/**
 * Captures EGL capabilities that influence HWUI swap behavior fallbacks.
 */
object EglHwuiCapabilityProbe {
    private const val TAG = "HWUI"
    private const val EGL_OPENGL_ES3_BIT_KHR = 0x40
    private const val EGL_SWAP_BEHAVIOR = 0x3093
    private const val EGL_BUFFER_PRESERVED = 0x3094
    private const val EGL_SWAP_BEHAVIOR_PRESERVED_BIT = 0x0400

    private val didProbe = AtomicBoolean(false)

    fun probeAndLogAsync() {
        if (!BuildConfig.DEBUG || !didProbe.compareAndSet(false, true)) {
            return
        }
        thread(name = "EglHwuiCapabilityProbe", isDaemon = true) {
            runCatching {
                logSnapshot(probe())
            }.onFailure { error ->
                Timber.tag(TAG).w(error, "EGL probe failed")
            }
        }
    }

    private fun probe(): Snapshot {
        val clientExtensions = parseExtensions(EGL14.eglQueryString(EGL14.EGL_NO_DISPLAY, EGL14.EGL_EXTENSIONS))
        val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        check(display != EGL14.EGL_NO_DISPLAY) { "Unable to acquire EGL display" }

        val version = IntArray(2)
        check(EGL14.eglInitialize(display, version, 0, version, 1)) { "eglInitialize failed" }

        try {
            val vendor = EGL14.eglQueryString(display, EGL14.EGL_VENDOR).orEmpty()
            val versionString = EGL14.eglQueryString(display, EGL14.EGL_VERSION).orEmpty()
            val displayExtensions = parseExtensions(EGL14.eglQueryString(display, EGL14.EGL_EXTENSIONS))

            val numConfigs = IntArray(1)
            // eglGetConfigs reports how many entries were written, not total, when size=0.
            // Request one entry first so numConfigs receives the total available config count.
            check(EGL14.eglGetConfigs(display, arrayOfNulls(1), 0, 1, numConfigs, 0)) { "eglGetConfigs(count) failed" }

            val configs = arrayOfNulls<EGLConfig>(numConfigs[0])
            check(EGL14.eglGetConfigs(display, configs, 0, configs.size, numConfigs, 0)) { "eglGetConfigs(list) failed" }

            val configStats = evaluateConfigs(display, configs.filterNotNull())

            return Snapshot(
                vendor = vendor,
                version = versionString,
                majorVersion = version[0],
                minorVersion = version[1],
                clientExtensions = clientExtensions,
                displayExtensions = displayExtensions,
                configStats = configStats,
            )
        } finally {
            EGL14.eglTerminate(display)
        }
    }

    private fun evaluateConfigs(display: android.opengl.EGLDisplay, configs: List<EGLConfig>): ConfigStats {
        var windowConfigCount = 0
        var preservedBitCount = 0
        var preservedBehaviorCount = 0
        var es3WindowCount = 0
        val samples = mutableListOf<String>()

        for (config in configs) {
            val surfaceType = getConfigAttrib(display, config, EGL14.EGL_SURFACE_TYPE)
            if ((surfaceType and EGL14.EGL_WINDOW_BIT) == 0) {
                continue
            }

            windowConfigCount += 1
            if ((surfaceType and EGL_SWAP_BEHAVIOR_PRESERVED_BIT) != 0) {
                preservedBitCount += 1
            }

            val renderableType = getConfigAttrib(display, config, EGL14.EGL_RENDERABLE_TYPE)
            if ((renderableType and EGL_OPENGL_ES3_BIT_KHR) != 0) {
                es3WindowCount += 1
            }

            val swapBehavior = getConfigAttribOrNull(display, config, EGL_SWAP_BEHAVIOR)
            if (swapBehavior == EGL_BUFFER_PRESERVED) {
                preservedBehaviorCount += 1
            }

            if (samples.size < 5) {
                val alphaSize = getConfigAttrib(display, config, EGL14.EGL_ALPHA_SIZE)
                samples += "surf=${describeSurfaceType(surfaceType)} alpha=$alphaSize renderable=0x${renderableType.toString(16)} swap=${describeSwapBehavior(swapBehavior)}"
            }
        }

        return ConfigStats(
            windowConfigCount = windowConfigCount,
            preservedBitCount = preservedBitCount,
            preservedBehaviorCount = preservedBehaviorCount,
            es3WindowCount = es3WindowCount,
            sampleConfigs = samples,
        )
    }

    private fun logSnapshot(snapshot: Snapshot) {
        val ext = snapshot.displayExtensions
        Timber.tag(TAG).i(
            "EGL display=%s (%d.%d) vendor=%s",
            snapshot.version,
            snapshot.majorVersion,
            snapshot.minorVersion,
            snapshot.vendor,
        )
        Timber.tag(TAG).i(
            "EGL swap extensions buffer_age=%s partial_update=%s khr_damage=%s ext_damage=%s preserved_swap=%s",
            hasExtension(ext, "EGL_EXT_buffer_age"),
            hasExtension(ext, "EGL_KHR_partial_update"),
            hasExtension(ext, "EGL_KHR_swap_buffers_with_damage"),
            hasExtension(ext, "EGL_EXT_swap_buffers_with_damage"),
            snapshot.configStats.preservedBitCount > 0,
        )
        Timber.tag(TAG).i(
            "EGL window configs=%d es3Window=%d preservedBit=%d preservedBehavior=%d",
            snapshot.configStats.windowConfigCount,
            snapshot.configStats.es3WindowCount,
            snapshot.configStats.preservedBitCount,
            snapshot.configStats.preservedBehaviorCount,
        )
        snapshot.configStats.sampleConfigs.forEachIndexed { index, sample ->
            Timber.tag(TAG).d("EGL config sample #%d %s", index + 1, sample)
        }
    }

    private fun getConfigAttrib(display: android.opengl.EGLDisplay, config: EGLConfig, attribute: Int): Int {
        val value = IntArray(1)
        if (!EGL14.eglGetConfigAttrib(display, config, attribute, value, 0)) {
            return 0
        }
        return value[0]
    }

    private fun getConfigAttribOrNull(display: android.opengl.EGLDisplay, config: EGLConfig, attribute: Int): Int? {
        val value = IntArray(1)
        return if (EGL14.eglGetConfigAttrib(display, config, attribute, value, 0)) value[0] else null
    }

    internal fun parseExtensions(rawExtensions: String?): Set<String> {
        if (rawExtensions.isNullOrBlank()) {
            return emptySet()
        }
        return rawExtensions
            .split(' ')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }

    internal fun hasExtension(extensions: Set<String>, name: String): Boolean {
        return extensions.contains(name)
    }

    internal fun describeSurfaceType(surfaceType: Int): String {
        val parts = mutableListOf<String>()
        if ((surfaceType and EGL14.EGL_WINDOW_BIT) != 0) parts += "WINDOW"
        if ((surfaceType and EGL14.EGL_PBUFFER_BIT) != 0) parts += "PBUFFER"
        if ((surfaceType and EGL14.EGL_PIXMAP_BIT) != 0) parts += "PIXMAP"
        if ((surfaceType and EGL_SWAP_BEHAVIOR_PRESERVED_BIT) != 0) parts += "PRESERVED_SWAP"
        return if (parts.isEmpty()) "NONE" else parts.joinToString("|")
    }

    internal fun describeSwapBehavior(value: Int?): String {
        return when (value) {
            null -> "UNAVAILABLE"
            EGL_BUFFER_PRESERVED -> "PRESERVED"
            EGL14.EGL_NONE -> "NONE"
            else -> "0x${value.toString(16)}"
        }
    }

    private data class Snapshot(
        val vendor: String,
        val version: String,
        val majorVersion: Int,
        val minorVersion: Int,
        val clientExtensions: Set<String>,
        val displayExtensions: Set<String>,
        val configStats: ConfigStats,
    )

    private data class ConfigStats(
        val windowConfigCount: Int,
        val preservedBitCount: Int,
        val preservedBehaviorCount: Int,
        val es3WindowCount: Int,
        val sampleConfigs: List<String>,
    )
}




