package app.gamegrub.ui.screen.xserver

import com.winlator.core.ProcessHelper

data class XServerPostExitHealthReport(
    val runningWineProcessCount: Int,
    val lingeringRuntimeThreadCount: Int,
    val lingeringRuntimeThreads: List<String>,
) {
    fun hasLeftovers(): Boolean {
        return runningWineProcessCount > 0 || lingeringRuntimeThreadCount > 0
    }
}

internal object XServerPostExitHealthChecker {
    private val runtimeThreadPrefixes: List<String> = listOf(
        "WinHandler-",
        "XConnectorEpoll:",
        "SteamPipeServer-",
    )

    fun check(
        wineProcessProvider: () -> List<String> = { ProcessHelper.listRunningWineProcesses() },
        threadNameProvider: () -> List<String> = {
            Thread.getAllStackTraces().keys.mapNotNull { thread ->
                thread.name
            }
        },
    ): XServerPostExitHealthReport {
        val winePids = wineProcessProvider()
        val lingeringThreads = threadNameProvider().filter { threadName ->
            runtimeThreadPrefixes.any { prefix -> threadName.startsWith(prefix) }
        }.distinct()

        return XServerPostExitHealthReport(
            runningWineProcessCount = winePids.size,
            lingeringRuntimeThreadCount = lingeringThreads.size,
            lingeringRuntimeThreads = lingeringThreads,
        )
    }
}

