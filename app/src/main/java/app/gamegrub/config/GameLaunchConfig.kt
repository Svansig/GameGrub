package app.gamegrub.config

import app.gamegrub.data.GameSource
import app.gamegrub.data.LibraryItem

data class GameLaunchConfig(
    val appId: String,
    val gameSource: GameSource,
    val executablePath: String,
    val workingDirectory: String,
    val environmentVariables: Map<String, String> = emptyMap(),
    val launchArguments: String = "",
    val containerConfig: ContainerConfig = ContainerConfig(),
)

data class ContainerConfig(
    val cpuAffinity: List<Int> = emptyList(),
    val memoryLimit: Long = 0,
    val gpuMode: GpuMode = GpuMode.AUTO,
    val audioMode: AudioMode = AudioMode.AUTO,
    val displayMode: DisplayMode = DisplayMode.AUTO,
    val winsyncMode: WinsyncMode = WinsyncMode.AUTO,
    val dxvkMode: DxvkMode = DxvkMode.AUTO,
    val mouseMode: MouseMode = MouseMode.AUTO,
    val keyboardMode: KeyboardMode = KeyboardMode.AUTO,
    val extraLaunchParams: String = "",
    val customEnvVars: Map<String, String> = emptyMap(),
)

enum class GpuMode { AUTO, INTEL, NVIDIA, AMD, DISABLE }
enum class AudioMode { AUTO, ON, OFF }
enum class DisplayMode { AUTO, FULLSCREEN, WINDOWED, BORDERLESS }
enum class WinsyncMode { AUTO, ON, OFF }
enum class DxvkMode { AUTO, ON, OFF, NOCACHE }
enum class MouseMode { AUTO, CAPTURE, TOUCH, HOVER }
enum class KeyboardMode { AUTO, ON, OFF }

fun ContainerConfig.toEnvVars(): Map<String, String> {
    return mapOf(
        "WINERCFG_CPU_AFFINITY" to cpuAffinity.joinToString(","),
        "WINERCFG_MEMORY_LIMIT" to memoryLimit.toString(),
        "WINERCFG_GPU_MODE" to gpuMode.name,
        "WINERCFG_AUDIO_MODE" to audioMode.name,
        "WINERCFG_DISPLAY_MODE" to displayMode.name,
        "WINERCFG_WINSYNC_MODE" to winsyncMode.name,
        "WINERCFG_DXVK_MODE" to dxvkMode.name,
        "WINERCFG_MOUSE_MODE" to mouseMode.name,
        "WINERCFG_KEYBOARD_MODE" to keyboardMode.name,
    ) + customEnvVars
}
