package app.gamegrub.ui.screen.xserver

import app.gamegrub.PrefManager
import app.gamegrub.ui.data.PerformanceHudConfig
import app.gamegrub.ui.data.PerformanceHudSize

/**
 * Reads and persists performance HUD preferences for the XServer screen.
 */
internal object XServerPerformanceHudConfigStore {
    fun load(): PerformanceHudConfig {
        return PerformanceHudConfig(
            showFrameRate = PrefManager.performanceHudShowFrameRate,
            showCpuUsage = PrefManager.performanceHudShowCpuUsage,
            showGpuUsage = PrefManager.performanceHudShowGpuUsage,
            showRamUsage = PrefManager.performanceHudShowRamUsage,
            showBatteryLevel = PrefManager.performanceHudShowBatteryLevel,
            showPowerDraw = PrefManager.performanceHudShowPowerDraw,
            showBatteryRuntime = PrefManager.performanceHudShowBatteryRuntime,
            showClockTime = PrefManager.performanceHudShowClockTime,
            showCpuTemperature = PrefManager.performanceHudShowCpuTemperature,
            showGpuTemperature = PrefManager.performanceHudShowGpuTemperature,
            showFrameRateGraph = PrefManager.performanceHudShowFrameRateGraph,
            showCpuUsageGraph = PrefManager.performanceHudShowCpuUsageGraph,
            showGpuUsageGraph = PrefManager.performanceHudShowGpuUsageGraph,
            backgroundOpacity = PrefManager.performanceHudBackgroundOpacity,
            colorIntensity = PrefManager.performanceHudColorIntensity,
            showTextOutline = PrefManager.performanceHudShowTextOutline,
            size = PerformanceHudSize.fromPrefValue(PrefManager.performanceHudSize),
        )
    }

    fun persist(config: PerformanceHudConfig) {
        PrefManager.performanceHudShowFrameRate = config.showFrameRate
        PrefManager.performanceHudShowCpuUsage = config.showCpuUsage
        PrefManager.performanceHudShowGpuUsage = config.showGpuUsage
        PrefManager.performanceHudShowRamUsage = config.showRamUsage
        PrefManager.performanceHudShowBatteryLevel = config.showBatteryLevel
        PrefManager.performanceHudShowPowerDraw = config.showPowerDraw
        PrefManager.performanceHudShowBatteryRuntime = config.showBatteryRuntime
        PrefManager.performanceHudShowClockTime = config.showClockTime
        PrefManager.performanceHudShowCpuTemperature = config.showCpuTemperature
        PrefManager.performanceHudShowGpuTemperature = config.showGpuTemperature
        PrefManager.performanceHudShowFrameRateGraph = config.showFrameRateGraph
        PrefManager.performanceHudShowCpuUsageGraph = config.showCpuUsageGraph
        PrefManager.performanceHudShowGpuUsageGraph = config.showGpuUsageGraph
        PrefManager.performanceHudBackgroundOpacity = config.backgroundOpacity
        PrefManager.performanceHudColorIntensity = config.colorIntensity
        PrefManager.performanceHudShowTextOutline = config.showTextOutline
        PrefManager.performanceHudSize = config.size.prefValue
    }
}

