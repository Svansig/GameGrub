package app.gamegrub.launch.error

import kotlinx.serialization.Serializable

@Serializable
enum class FailureClass {
    PROCESS_SPAWN,
    BACKEND_INIT,
    GRAPHICS_INIT,
    CONTAINER_SETUP,
    MISSING_DRIVER,
    CORRUPTED_CACHE,
    TIMEOUT,
    UNKNOWN,
}

@Serializable
data class LaunchFailureRecord(
    val sessionId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val failureClass: FailureClass,
    val detectedRootCause: String? = null,
    val stderrSnippet: String? = null,
    val stdoutSnippet: String? = null,
    val processExitCode: Int? = null,
    val timeToFailureMs: Long? = null,
    val recoveryAction: RecoveryAction = RecoveryAction.NONE,
    val context: Map<String, String> = emptyMap(),
)

@Serializable
enum class RecoveryAction {
    NONE,
    RETRY_SAME_CONFIG,
    CACHE_INVALIDATION,
    RE_EXTRACT,
    FALLBACK_PROFILE,
    FALLBACK_RUNTIME,
    FALLBACK_DRIVER,
    CONTAINER_RESET,
    USER_INTERVENTION_REQUIRED,
}

object LaunchFailureClassifier {
    fun classify(
        exitCode: Int?,
        stderr: String?,
        stdout: String?,
        timeToFailureMs: Long?,
    ): Pair<FailureClass, String?> {
        val stderrLower = stderr?.lowercase() ?: ""
        val stdoutLower = stdout?.lowercase() ?: ""

        if (timeToFailureMs != null && timeToFailureMs > 300_000) {
            return FailureClass.TIMEOUT to "Launch exceeded 5 minute limit"
        }

        if (exitCode == null) {
            return FailureClass.PROCESS_SPAWN to "Process failed to spawn"
        }

        if (exitCode == 127 || stderrLower.contains("no such file or directory")) {
            return FailureClass.PROCESS_SPAWN to "Executable not found"
        }

        if (stderrLower.contains("wine: cannot find") ||
            stdoutLower.contains("wine: cannot find") ||
            stderrLower.contains("proton: cannot find")
        ) {
            return FailureClass.MISSING_DRIVER to "Wine/Proton not found"
        }

        if (stderrLower.contains("vulkan") &&
            (stderrLower.contains("failed") || stderrLower.contains("error"))
        ) {
            return FailureClass.GRAPHICS_INIT to "Vulkan initialization failed"
        }

        if (stderrLower.contains("dxvk") && stderrLower.contains("failed")) {
            return FailureClass.GRAPHICS_INIT to "DXVK initialization failed"
        }

        if (stderrLower.contains("libdxvk") || stderrLower.contains("dxvk state cache")) {
            return FailureClass.CORRUPTED_CACHE to "DXVK cache corrupted"
        }

        if (stderrLower.contains("mesa shader cache") || stderrLower.contains("shader cache")) {
            return FailureClass.CORRUPTED_CACHE to "Shader cache corrupted"
        }

        if (exitCode == 1 && stderrLower.contains(" wineserver not found")) {
            return FailureClass.BACKEND_INIT to "Wine server initialization failed"
        }

        if (exitCode == 1 && (stderrLower.contains("prefix") || stderrLower.contains(".wine"))) {
            return FailureClass.CONTAINER_SETUP to "Wine prefix error"
        }

        if (stderrLower.contains("driver") && stderrLower.contains("not found")) {
            return FailureClass.MISSING_DRIVER to "Graphics driver not found"
        }

        if (stderrLower.contains("segfault") || stderrLower.contains("signal 11")) {
            return FailureClass.BACKEND_INIT to "Process crashed (segfault)"
        }

        return FailureClass.UNKNOWN to null
    }

    fun recommendRecovery(failureClass: FailureClass): RecoveryAction {
        return when (failureClass) {
            FailureClass.PROCESS_SPAWN -> RecoveryAction.RETRY_SAME_CONFIG
            FailureClass.BACKEND_INIT -> RecoveryAction.CACHE_INVALIDATION
            FailureClass.GRAPHICS_INIT -> RecoveryAction.FALLBACK_DRIVER
            FailureClass.CONTAINER_SETUP -> RecoveryAction.CONTAINER_RESET
            FailureClass.MISSING_DRIVER -> RecoveryAction.RE_EXTRACT
            FailureClass.CORRUPTED_CACHE -> RecoveryAction.CACHE_INVALIDATION
            FailureClass.TIMEOUT -> RecoveryAction.RETRY_SAME_CONFIG
            FailureClass.UNKNOWN -> RecoveryAction.NONE
        }
    }
}
