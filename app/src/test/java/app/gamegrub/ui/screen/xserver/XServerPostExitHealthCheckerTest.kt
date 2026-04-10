package app.gamegrub.ui.screen.xserver

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class XServerPostExitHealthCheckerTest {

    @Test
    fun check_reportsNoLeftovers_whenNoWineProcessAndNoRuntimeThreads() {
        val report = XServerPostExitHealthChecker.check(
            wineProcessProvider = { emptyList() },
            threadNameProvider = { listOf("main", "FinalizerDaemon") },
        )

        assertEquals(0, report.runningWineProcessCount)
        assertEquals(0, report.lingeringRuntimeThreadCount)
        assertFalse(report.hasLeftovers())
    }

    @Test
    fun check_reportsLeftovers_whenRuntimeThreadsMatchPrefixes() {
        val report = XServerPostExitHealthChecker.check(
            wineProcessProvider = { listOf("123") },
            threadNameProvider = {
                listOf(
                    "main",
                    "WinHandler-Receive",
                    "XConnectorEpoll:/tmp/.X11-unix/X0",
                    "SteamPipeServer-Accept",
                )
            },
        )

        assertEquals(1, report.runningWineProcessCount)
        assertEquals(3, report.lingeringRuntimeThreadCount)
        assertTrue(report.hasLeftovers())
    }

    @Test
    fun teardownReport_hasFailures_whenPostExitHealthHasLeftovers() {
        val report = XServerTeardownReport(
            appId = "STEAM_250760",
            totalDurationMs = 100,
            phases = listOf(
                XServerTeardownPhase("StopEnvironmentComponents", 25, true),
            ),
            postExitHealthReport = XServerPostExitHealthReport(
                runningWineProcessCount = 1,
                lingeringRuntimeThreadCount = 0,
                lingeringRuntimeThreads = emptyList(),
            ),
        )

        assertTrue(report.hasFailures())
        assertTrue(report.toLogLine().contains("leftoverWine=1"))
    }
}

