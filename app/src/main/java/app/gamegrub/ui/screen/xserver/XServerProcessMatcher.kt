package app.gamegrub.ui.screen.xserver

import com.winlator.core.WineUtils
import com.winlator.xserver.Window
import java.util.Locale

internal object XServerProcessMatcher {
    private val coreWineProcesses = setOf(
        "wineserver",
        "services",
        "start",
        "winhandler",
        "tabtip",
        "explorer",
        "winedevice",
        "svchost",
    )

    fun normalizeProcessName(name: String): String {
        val trimmed = name.trim().trim('"')
        val base = trimmed.substringAfterLast('/').substringAfterLast('\\')
        val lower = base.lowercase(Locale.getDefault())
        return if (lower.endsWith(".exe")) lower.removeSuffix(".exe") else lower
    }

    fun extractExecutableBasename(path: String): String {
        if (path.isBlank()) {
            return ""
        }
        return normalizeProcessName(path)
    }

    fun windowMatchesExecutable(window: Window, targetExecutable: String): Boolean {
        if (targetExecutable.isBlank()) {
            return false
        }

        val normalizedTarget = normalizeProcessName(targetExecutable)
        val candidates = listOf(window.name, window.className)
        return candidates.any { candidate ->
            candidate.split('\u0000')
                .asSequence()
                .map { normalizeProcessName(it) }
                .any { it == normalizedTarget }
        }
    }

    fun buildEssentialProcessAllowlist(): Set<String> {
        val essentialServices = WineUtils.getEssentialServiceNames()
            .map { normalizeProcessName(it) }
        return (essentialServices + coreWineProcesses).toSet()
    }
}
