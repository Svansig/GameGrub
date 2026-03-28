package app.gamegrub.utils.device

import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.window.core.layout.WindowWidthSizeClass

object DeviceUtils {
    // Functions related to the device and view

    fun isViewWide(windowSizeClass: WindowAdaptiveInfo): Boolean {
        return windowSizeClass.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.MEDIUM ||
                windowSizeClass.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.EXPANDED
    }
}
