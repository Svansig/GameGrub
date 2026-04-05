package app.gamegrub.ui.component.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.gamegrub.R
import app.gamegrub.ui.component.settings.SettingsListDropdown
import app.gamegrub.ui.component.settings.SettingsMultiListDropdown
import app.gamegrub.ui.theme.settingsTileColors
import app.gamegrub.ui.theme.settingsTileColorsAlt
import com.alorma.compose.settings.ui.SettingsGroup
import com.alorma.compose.settings.ui.SettingsSwitch
import com.winlator.container.Container
import com.winlator.contents.ContentProfile
import com.winlator.core.KeyValueSet
import com.winlator.core.StringUtils
import com.winlator.core.envvars.EnvVars
import kotlin.math.roundToInt

@Composable
fun GraphicsTabContent(state: ContainerConfigState) {
    val config = state.config.value
    SettingsGroup {
        if (config.containerVariant.equals(Container.BIONIC, ignoreCase = true)) {
            // Bionic: Graphics Driver (Wrapper/Wrapper-v2)
            SettingsListDropdown(
                colors = settingsTileColors(),
                title = { Text(text = stringResource(R.string.graphics_driver)) },
                value = state.bionicDriverIndex.intValue,
                items = state.bionicGraphicsDrivers,
                onItemSelected = { idx ->
                    state.bionicDriverIndex.intValue = idx
                    state.config.value = config.copy(graphicsDriver = StringUtils.parseIdentifier(state.bionicGraphicsDrivers[idx]))
                },
            )
            // Bionic: Graphics Driver Version (stored in graphicsDriverConfig.version; list from manifest + installed)
            SettingsListDropdown(
                colors = settingsTileColors(),
                title = { Text(text = stringResource(R.string.graphics_driver_version)) },
                value = state.wrapperVersionIndex.intValue.coerceIn(0, (state.wrapperOptions.labels.size - 1).coerceAtLeast(0)),
                items = state.wrapperOptions.labels,
                itemMuted = state.wrapperOptions.muted,
                onItemSelected = { idx ->
                    val selectedId = state.wrapperOptions.ids.getOrNull(idx).orEmpty()
                    val isManifestNotInstalled = state.wrapperOptions.muted.getOrNull(idx) == true
                    val manifestEntry = state.wrapperManifestById[selectedId]
                    if (isManifestNotInstalled && manifestEntry != null) {
                        state.launchManifestDriverInstall(manifestEntry) {
                            val cfg = KeyValueSet(config.graphicsDriverConfig)
                            cfg.put("version", state.wrapperOptions.labels[idx])
                            state.config.value = config.copy(graphicsDriverConfig = cfg.toString())
                        }
                        return@SettingsListDropdown
                    }
                    state.wrapperVersionIndex.intValue = idx
                    val cfg = KeyValueSet(config.graphicsDriverConfig)
                    cfg.put("version", selectedId.ifEmpty { state.wrapperOptions.labels[idx] })
                    state.config.value = config.copy(graphicsDriverConfig = cfg.toString())
                },
            )
            DxWrapperSection(state)
            // Bionic: Exposed Vulkan Extensions (same UI as Vortek)
            SettingsMultiListDropdown(
                colors = settingsTileColors(),
                title = { Text(text = stringResource(R.string.exposed_vulkan_extensions)) },
                values = state.exposedExtIndices.value,
                items = state.gpuExtensions,
                fallbackDisplay = "all",
                onItemSelected = { idx ->
                    val current = state.exposedExtIndices.value
                    state.exposedExtIndices.value =
                        if (current.contains(idx)) current.filter { it != idx } else current + idx
                    val cfg = KeyValueSet(config.graphicsDriverConfig)
                    val allSelected = state.exposedExtIndices.value.size == state.gpuExtensions.size
                    if (allSelected) {
                        cfg.put("exposedDeviceExtensions", "all")
                    } else {
                        cfg.put(
                            "exposedDeviceExtensions",
                            state.exposedExtIndices.value.sorted().joinToString("|") { state.gpuExtensions[it] },
                        )
                    }
                    val blacklisted = if (allSelected) {
                        ""
                    } else {
                        state.gpuExtensions.indices
                            .filter { it !in state.exposedExtIndices.value }
                            .sorted()
                            .joinToString(",") { state.gpuExtensions[it] }
                    }
                    cfg.put("blacklistedExtensions", blacklisted)
                    state.config.value = config.copy(graphicsDriverConfig = cfg.toString())
                },
            )
            // Bionic: Max Device Memory (same as Vortek)
            run {
                val memValues = listOf("0", "512", "1024", "2048", "4096")
                val memLabels = listOf("0 MB", "512 MB", "1024 MB", "2048 MB", "4096 MB")
                SettingsListDropdown(
                    colors = settingsTileColors(),
                    title = { Text(text = stringResource(R.string.max_device_memory)) },
                    value = state.maxDeviceMemoryIndex.intValue.coerceIn(0, memValues.lastIndex),
                    items = memLabels,
                    onItemSelected = { idx ->
                        state.maxDeviceMemoryIndex.intValue = idx
                        val cfg = KeyValueSet(config.graphicsDriverConfig)
                        cfg.put("maxDeviceMemory", memValues[idx])
                        state.config.value = config.copy(graphicsDriverConfig = cfg.toString())
                    },
                )
            }
            // Bionic: Use Adrenotools Turnip
            SettingsSwitch(
                colors = settingsTileColorsAlt(),
                title = { Text(text = stringResource(R.string.use_adrenotools_turnip)) },
                state = state.adrenotoolsTurnipChecked.value,
                onCheckedChange = { checked ->
                    state.adrenotoolsTurnipChecked.value = checked
                    val cfg = KeyValueSet(config.graphicsDriverConfig)
                    cfg.put("adrenotoolsTurnip", if (checked) "1" else "0")
                    state.config.value = config.copy(graphicsDriverConfig = cfg.toString())
                },
            )
            if (config.wineVersion.contains("arm64ec", true)) {
                SettingsListDropdown(
                    colors = settingsTileColors(),
                    title = { Text(text = stringResource(R.string.present_modes)) },
                    value = state.presentModeIndex.intValue.coerceIn(0, state.presentModes.lastIndex.coerceAtLeast(0)),
                    items = state.presentModes,
                    onItemSelected = { idx ->
                        state.presentModeIndex.intValue = idx
                        val cfg = KeyValueSet(config.graphicsDriverConfig)
                        cfg.put("presentMode", state.presentModes[idx])
                        state.config.value = config.copy(graphicsDriverConfig = cfg.toString())
                    },
                )
                SettingsListDropdown(
                    colors = settingsTileColors(),
                    title = { Text(text = stringResource(R.string.resource_type)) },
                    value = state.resourceTypeIndex.intValue.coerceIn(0, state.resourceTypes.lastIndex.coerceAtLeast(0)),
                    items = state.resourceTypes,
                    onItemSelected = { idx ->
                        state.resourceTypeIndex.intValue = idx
                        val cfg = KeyValueSet(config.graphicsDriverConfig)
                        cfg.put("resourceType", state.resourceTypes[idx])
                        state.config.value = config.copy(graphicsDriverConfig = cfg.toString())
                    },
                )
                SettingsListDropdown(
                    colors = settingsTileColors(),
                    title = { Text(text = stringResource(R.string.bcn_emulation)) },
                    value = state.bcnEmulationIndex.intValue.coerceIn(0, state.bcnEmulationEntries.lastIndex.coerceAtLeast(0)),
                    items = state.bcnEmulationEntries,
                    onItemSelected = { idx ->
                        state.bcnEmulationIndex.intValue = idx
                        val cfg = KeyValueSet(config.graphicsDriverConfig)
                        cfg.put("bcnEmulation", state.bcnEmulationEntries[idx])
                        state.config.value = config.copy(graphicsDriverConfig = cfg.toString())
                    },
                )
                SettingsListDropdown(
                    colors = settingsTileColors(),
                    title = { Text(text = stringResource(R.string.bcn_emulation_type)) },
                    value = state.bcnEmulationTypeIndex.intValue.coerceIn(0, state.bcnEmulationTypeEntries.lastIndex.coerceAtLeast(0)),
                    items = state.bcnEmulationTypeEntries,
                    onItemSelected = { i ->
                        state.bcnEmulationTypeIndex.intValue = i
                        val cfg = KeyValueSet(config.graphicsDriverConfig)
                        cfg.put("bcnEmulationType", state.bcnEmulationTypeEntries[i])
                        state.config.value = config.copy(graphicsDriverConfig = cfg.toString())
                    },
                )
                // Sharpness (vkBasalt)
                SettingsListDropdown(
                    colors = settingsTileColors(),
                    title = { Text(text = stringResource(R.string.sharpness_effect)) },
                    value = state.sharpnessEffectIndex.intValue.coerceIn(0, state.sharpnessEffects.lastIndex.coerceAtLeast(0)),
                    items = state.sharpnessDisplayItems,
                    onItemSelected = { idx ->
                        state.sharpnessEffectIndex.intValue = idx
                        state.config.value = config.copy(sharpnessEffect = state.sharpnessEffects[idx])
                    },
                )
                val selectedBoost = state.sharpnessEffects
                    .getOrNull(state.sharpnessEffectIndex.intValue)
                    ?.equals("None", ignoreCase = true)
                    ?.not() ?: false
                if (selectedBoost) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(text = stringResource(R.string.sharpness_level))
                        Slider(
                            value = state.sharpnessLevel.intValue.toFloat(),
                            onValueChange = { newValue ->
                                val clamped = newValue.roundToInt().coerceIn(0, 100)
                                state.sharpnessLevel.intValue = clamped
                                state.config.value = config.copy(sharpnessLevel = clamped)
                            },
                            valueRange = 0f..100f,
                        )
                        Text(text = "${state.sharpnessLevel.intValue}%")
                    }
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(text = stringResource(R.string.sharpness_denoise))
                        Slider(
                            value = state.sharpnessDenoise.intValue.toFloat(),
                            onValueChange = { newValue ->
                                val clamped = newValue.roundToInt().coerceIn(0, 100)
                                state.sharpnessDenoise.intValue = clamped
                                state.config.value = config.copy(sharpnessDenoise = clamped)
                            },
                            valueRange = 0f..100f,
                        )
                        Text(text = "${state.sharpnessDenoise.intValue}%")
                    }
                }
            }
        } else {
            // Non-bionic: existing driver/version UI and Vortek-specific options
            SettingsListDropdown(
                colors = settingsTileColors(),
                title = { Text(text = stringResource(R.string.graphics_driver)) },
                value = state.graphicsDriverIndex.intValue,
                items = state.graphicsDrivers.value,
                onItemSelected = {
                    state.graphicsDriverIndex.intValue = it
                    state.graphicsDriverVersionIndex.intValue = 0
                    state.config.value = config.copy(
                        graphicsDriver = StringUtils.parseIdentifier(state.graphicsDrivers.value[it]),
                        graphicsDriverVersion = "",
                    )
                },
            )
            SettingsListDropdown(
                colors = settingsTileColors(),
                title = { Text(text = stringResource(R.string.graphics_driver_version)) },
                value = state.graphicsDriverVersionIndex.intValue,
                items = state.getVersionsForDriver(),
                onItemSelected = {
                    state.graphicsDriverVersionIndex.intValue = it
                    val selectedVersion = if (it == 0) "" else state.getVersionsForDriver()[it]
                    state.config.value = config.copy(graphicsDriverVersion = selectedVersion)
                },
            )
            DxWrapperSection(state)
            // Vortek/Adreno specific settings
            run {
                val driverType =
                    StringUtils.parseIdentifier(state.graphicsDrivers.value.getOrNull(state.graphicsDriverIndex.intValue).orEmpty())
                val isVortekLike =
                    config.containerVariant == Container.GLIBC &&
                        (driverType == "vortek" || driverType == "adreno" || driverType == "sd-8-elite")
                if (isVortekLike) {
                    val vkVersions = listOf("1.0", "1.1", "1.2", "1.3")
                    SettingsListDropdown(
                        colors = settingsTileColors(),
                        title = { Text(text = stringResource(R.string.vulkan_version)) },
                        value = state.vkMaxVersionIndex.intValue.coerceIn(0, 3),
                        items = vkVersions,
                        onItemSelected = { idx ->
                            state.vkMaxVersionIndex.intValue = idx
                            val cfg = KeyValueSet(config.graphicsDriverConfig)
                            cfg.put("vkMaxVersion", vkVersions[idx])
                            state.config.value = config.copy(graphicsDriverConfig = cfg.toString())
                        },
                    )
                    SettingsMultiListDropdown(
                        colors = settingsTileColors(),
                        title = { Text(text = stringResource(R.string.exposed_vulkan_extensions)) },
                        values = state.exposedExtIndices.value,
                        items = state.gpuExtensions,
                        fallbackDisplay = "all",
                        onItemSelected = { idx ->
                            val current = state.exposedExtIndices.value
                            state.exposedExtIndices.value =
                                if (current.contains(idx)) current.filter { it != idx } else current + idx
                            val cfg = KeyValueSet(config.graphicsDriverConfig)
                            val allSelected = state.exposedExtIndices.value.size == state.gpuExtensions.size
                            if (allSelected) {
                                cfg.put("exposedDeviceExtensions", "all")
                            } else {
                                cfg.put(
                                    "exposedDeviceExtensions",
                                    state.exposedExtIndices.value.sorted().joinToString("|") { state.gpuExtensions[it] },
                                )
                            }
                            val blacklisted = if (allSelected) {
                                ""
                            } else {
                                state.gpuExtensions.indices
                                    .filter { it !in state.exposedExtIndices.value }
                                    .sorted()
                                    .joinToString(",") { state.gpuExtensions[it] }
                            }
                            cfg.put("blacklistedExtensions", blacklisted)
                            state.config.value = config.copy(graphicsDriverConfig = cfg.toString())
                        },
                    )
                    val imageSizes = listOf("64", "128", "256", "512", "1024")
                    val imageLabels = listOf("64", "128", "256", "512", "1024").map { "$it MB" }
                    SettingsListDropdown(
                        colors = settingsTileColors(),
                        title = { Text(text = stringResource(R.string.image_cache_size)) },
                        value = state.imageCacheIndex.intValue.coerceIn(0, imageSizes.lastIndex),
                        items = imageLabels,
                        onItemSelected = { idx ->
                            state.imageCacheIndex.intValue = idx
                            val cfg = KeyValueSet(config.graphicsDriverConfig)
                            cfg.put("imageCacheSize", imageSizes[idx])
                            state.config.value = config.copy(graphicsDriverConfig = cfg.toString())
                        },
                    )
                    val memValues = listOf("0", "512", "1024", "2048", "4096")
                    val memLabels = listOf("0 MB", "512 MB", "1024 MB", "2048 MB", "4096 MB")
                    SettingsListDropdown(
                        colors = settingsTileColors(),
                        title = { Text(text = stringResource(R.string.max_device_memory)) },
                        value = state.maxDeviceMemoryIndex.intValue.coerceIn(0, memValues.lastIndex),
                        items = memLabels,
                        onItemSelected = { idx ->
                            state.maxDeviceMemoryIndex.intValue = idx
                            val cfg = KeyValueSet(config.graphicsDriverConfig)
                            cfg.put("maxDeviceMemory", memValues[idx])
                            state.config.value = config.copy(graphicsDriverConfig = cfg.toString())
                        },
                    )
                }
            }
        }
        SettingsSwitch(
            colors = settingsTileColorsAlt(),
            title = { Text(text = stringResource(R.string.use_dri3)) },
            subtitle = { Text(text = stringResource(R.string.use_dri3_description)) },
            state = config.useDRI3,
            onCheckedChange = {
                state.config.value = config.copy(useDRI3 = it)
            },
        )
    }
}

@Composable
private fun DxWrapperSection(state: ContainerConfigState) {
    val config = state.config.value
    SettingsListDropdown(
        colors = settingsTileColors(),
        title = { Text(text = stringResource(R.string.dx_wrapper)) },
        value = state.dxWrapperIndex.intValue,
        items = state.dxWrappers,
        onItemSelected = {
            state.dxWrapperIndex.intValue = it
            state.config.value = config.copy(dxwrapper = StringUtils.parseIdentifier(state.dxWrappers[it]))
        },
    )
    // DXVK Version Dropdown (conditionally visible and constrained)
    run {
        val context = state.currentDxvkContext()
        val isVKD3D = StringUtils.parseIdentifier(state.dxWrappers.getOrNull(state.dxWrapperIndex.intValue).orEmpty()) == "vkd3d"
        if (!isVKD3D) {
            val items = context.labels
            val itemIds = context.ids
            val itemMuted = context.muted
            SettingsListDropdown(
                colors = settingsTileColors(),
                title = { Text(text = stringResource(R.string.dxvk_version)) },
                value = state.dxvkVersionIndex.intValue.coerceIn(0, (items.size - 1).coerceAtLeast(0)),
                items = items,
                itemMuted = itemMuted,
                onItemSelected = {
                    state.dxvkVersionIndex.intValue = it
                    val selectedId = itemIds.getOrNull(it).orEmpty()
                    val isManifestNotInstalled = state.isBionicVariant && itemMuted.getOrNull(it) == true
                    val manifestEntry = if (state.isBionicVariant) state.dxvkManifestById[selectedId] else null
                    if (isManifestNotInstalled && manifestEntry != null) {
                        state.launchManifestContentInstall(
                            manifestEntry,
                            ContentProfile.ContentType.CONTENT_TYPE_DXVK,
                        ) {
                            val currentConfig = KeyValueSet(config.dxwrapperConfig)
                            currentConfig.put("version", selectedId)
                            if (selectedId.contains("async", ignoreCase = true)) {
                                currentConfig.put("async", "1")
                            } else {
                                currentConfig.put("async", "0")
                            }
                            if (selectedId.contains("gplasync", ignoreCase = true)) {
                                currentConfig.put("asyncCache", "1")
                            } else {
                                currentConfig.put("asyncCache", "0")
                            }
                            state.config.value = config.copy(dxwrapperConfig = currentConfig.toString())
                        }
                        return@SettingsListDropdown
                    }
                    val version = selectedId.ifEmpty { StringUtils.parseIdentifier(items.getOrNull(it).orEmpty()) }
                    val currentConfig = KeyValueSet(config.dxwrapperConfig)
                    currentConfig.put("version", version)
                    val envVarsSet = EnvVars(config.envVars)
                    if (version.contains("async", ignoreCase = true)) {
                        currentConfig.put("async", "1")
                    } else {
                        currentConfig.put("async", "0")
                    }
                    if (version.contains("gplasync", ignoreCase = true)) {
                        currentConfig.put("asyncCache", "1")
                    } else {
                        currentConfig.put("asyncCache", "0")
                    }
                    state.config.value =
                        config.copy(dxwrapperConfig = currentConfig.toString(), envVars = envVarsSet.toString())
                },
            )
        } else {
            // Ensure default version for vortek-like when hidden
            val driverType =
                StringUtils.parseIdentifier(state.graphicsDrivers.value.getOrNull(state.graphicsDriverIndex.intValue).orEmpty())
            val isVortekLike =
                config.containerVariant == Container.GLIBC &&
                    (driverType == "vortek" || driverType == "adreno" || driverType == "sd-8-elite")
            val version = if (isVortekLike) "1.10.3" else "2.4.1"
            val currentConfig = KeyValueSet(config.dxwrapperConfig)
            currentConfig.put("version", version)
            state.config.value = config.copy(dxwrapperConfig = currentConfig.toString())
        }
    }
    // VKD3D Version UI (visible only when VKD3D selected)
    run {
        val isVKD3D = StringUtils.parseIdentifier(state.dxWrappers.getOrNull(state.dxWrapperIndex.intValue).orEmpty()) == "vkd3d"
        if (isVKD3D) {
            val label = "VKD3D Version"
            val availableVersions = if (state.isBionicVariant) state.vkd3dOptions.labels else state.vkd3dVersionsBase
            val availableIds = if (state.isBionicVariant) state.vkd3dOptions.ids else state.vkd3dVersionsBase
            val availableMuted = if (state.isBionicVariant) state.vkd3dOptions.muted else null
            val selectedVersion =
                KeyValueSet(config.dxwrapperConfig).get("vkd3dVersion").ifEmpty { state.vkd3dForcedVersion() }
            val selectedIndex = availableIds.indexOf(selectedVersion).coerceAtLeast(0)

            SettingsListDropdown(
                colors = settingsTileColors(),
                title = { Text(text = label) },
                value = selectedIndex,
                items = availableVersions,
                itemMuted = availableMuted,
                onItemSelected = { idx ->
                    val selectedId = availableIds.getOrNull(idx).orEmpty()
                    val isManifestNotInstalled = state.isBionicVariant && availableMuted?.getOrNull(idx) == true
                    val manifestEntry = if (state.isBionicVariant) state.vkd3dManifestById[selectedId] else null
                    if (isManifestNotInstalled && manifestEntry != null) {
                        state.launchManifestContentInstall(
                            manifestEntry,
                            ContentProfile.ContentType.CONTENT_TYPE_VKD3D,
                        ) {
                            val currentConfig = KeyValueSet(config.dxwrapperConfig)
                            currentConfig.put("vkd3dVersion", selectedId)
                            state.config.value = config.copy(dxwrapperConfig = currentConfig.toString())
                        }
                        return@SettingsListDropdown
                    }
                    val currentConfig = KeyValueSet(config.dxwrapperConfig)
                    currentConfig.put("vkd3dVersion", selectedId.ifEmpty { availableVersions.getOrNull(idx).orEmpty() })
                    state.config.value = config.copy(dxwrapperConfig = currentConfig.toString())
                },
            )

            val featureLevels = listOf("12_2", "12_1", "12_0", "11_1", "11_0")
            val cfg = KeyValueSet(config.dxwrapperConfig)
            val currentLevel = cfg.get("vkd3dFeatureLevel", "12_1")
            val currentLevelIndex = featureLevels.indexOf(currentLevel).coerceAtLeast(0)
            SettingsListDropdown(
                colors = settingsTileColors(),
                title = { Text(text = stringResource(R.string.vkd3d_feature_level)) },
                value = currentLevelIndex,
                items = featureLevels,
                onItemSelected = {
                    val selected = featureLevels[it]
                    val currentConfig = KeyValueSet(config.dxwrapperConfig)
                    currentConfig.put("vkd3dFeatureLevel", selected)
                    state.config.value = config.copy(dxwrapperConfig = currentConfig.toString())
                },
            )
        }
    }
}
