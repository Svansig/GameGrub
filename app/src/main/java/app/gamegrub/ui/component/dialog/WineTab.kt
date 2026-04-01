package app.gamegrub.ui.component.dialog

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.gamegrub.R
import app.gamegrub.ui.component.settings.SettingsListDropdown
import app.gamegrub.ui.theme.settingsTileColors
import app.gamegrub.ui.theme.settingsTileColorsAlt
import com.alorma.compose.settings.ui.SettingsGroup
import com.alorma.compose.settings.ui.SettingsSwitch
import com.winlator.core.StringUtils

@Composable
fun WineTabContent(state: ContainerConfigState) {
    val config = state.config.value
    val gpuCardsValues = state.gpuCards.values.toList()
    SettingsGroup {
        SettingsListDropdown(
            colors = settingsTileColors(),
            title = { Text(text = stringResource(R.string.renderer)) },
            value = state.gpuNameIndex.intValue,
            items = state.gpuCards.values.map { it.name },
            onItemSelected = {
                state.gpuNameIndex.intValue = it
                val cfg = com.winlator.core.KeyValueSet(config.graphicsDriverConfig)
                cfg.put("gpuName", gpuCardsValues[it].deviceId)
                state.config.value = config.copy(
                    videoPciDeviceID = gpuCardsValues[it].deviceId,
                    graphicsDriverConfig = cfg.toString(),
                )
            },
        )
        SettingsListDropdown(
            colors = settingsTileColors(),
            title = { Text(text = stringResource(R.string.gpu_name)) },
            value = state.gpuNameIndex.intValue,
            items = state.gpuCards.values.map { it.name },
            onItemSelected = {
                state.gpuNameIndex.intValue = it
                state.config.value = config.copy(videoPciDeviceID = gpuCardsValues[it].deviceId)
            },
        )
        SettingsListDropdown(
            colors = settingsTileColors(),
            title = { Text(text = stringResource(R.string.offscreen_rendering_mode)) },
            value = state.renderingModeIndex.intValue,
            items = state.renderingModes,
            onItemSelected = {
                state.renderingModeIndex.intValue = it
                state.config.value = config.copy(offScreenRenderingMode = state.renderingModes[it].lowercase())
            },
        )
        SettingsListDropdown(
            colors = settingsTileColors(),
            title = { Text(text = stringResource(R.string.video_memory_size)) },
            value = state.videoMemIndex.intValue,
            items = state.videoMemSizes,
            onItemSelected = {
                state.videoMemIndex.intValue = it
                state.config.value = config.copy(videoMemorySize = StringUtils.parseNumber(state.videoMemSizes[it]))
            },
        )
        SettingsSwitch(
            colors = settingsTileColorsAlt(),
            title = { Text(text = stringResource(R.string.enable_csmt)) },
            state = config.csmt,
            onCheckedChange = { state.config.value = config.copy(csmt = it) },
        )
        SettingsSwitch(
            colors = settingsTileColorsAlt(),
            title = { Text(text = stringResource(R.string.enable_strict_shader_math)) },
            state = config.strictShaderMath,
            onCheckedChange = { state.config.value = config.copy(strictShaderMath = it) },
        )
        SettingsListDropdown(
            colors = settingsTileColors(),
            title = { Text(text = stringResource(R.string.mouse_warp_override)) },
            value = state.mouseWarpIndex.intValue,
            items = state.mouseWarps,
            onItemSelected = {
                state.mouseWarpIndex.intValue = it
                state.config.value = config.copy(mouseWarpOverride = state.mouseWarps[it].lowercase())
            },
        )
    }
}
