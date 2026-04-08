package app.gamegrub.container.manifest

import kotlinx.serialization.Serializable

@Serializable
data class ContainerManifest(
    val id: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis(),
    val gameId: String,
    val gamePlatform: String,
    val baseId: String,
    val runtimeId: String,
    val driverId: String? = null,
    val profileId: String? = null,
    val configuration: ContainerConfiguration = ContainerConfiguration(),
    val userOverrides: Map<String, String> = emptyMap(),
) {
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        if (id.isBlank()) errors.add("ContainerManifest.id cannot be blank")
        if (name.isBlank()) errors.add("ContainerManifest.name cannot be blank")
        if (gameId.isBlank()) errors.add("ContainerManifest.gameId cannot be blank")
        if (gamePlatform.isBlank()) errors.add("ContainerManifest.gamePlatform cannot be blank")
        if (baseId.isBlank()) errors.add("ContainerManifest.baseId cannot be blank")
        if (runtimeId.isBlank()) errors.add("ContainerManifest.runtimeId cannot be blank")
        return errors
    }

    fun isValid(): Boolean = validate().isEmpty()
}

@Serializable
data class ContainerConfiguration(
    val screenSize: String = "1280x720",
    val cpuList: String = "0-7",
    val cpuListWoW64: String = "0-3",
    val wow64Mode: Boolean = true,
    val renderer: String = "gl",
    val csmt: Boolean = true,
    val useDRI3: Boolean = true,
    val audioDriver: String = "pulse",
    val mouseWarpOverride: String = "disable",
    val sdlControllerAPI: Boolean = true,
    val useSteamInput: Boolean = false,
    val enableXInput: Boolean = true,
    val enableDInput: Boolean = true,
    val containerVariant: String = "glibc",
    val wineVersion: String = "main",
    val emulator: String = "box64",
)