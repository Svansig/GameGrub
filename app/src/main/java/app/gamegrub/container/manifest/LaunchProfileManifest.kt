package app.gamegrub.container.manifest

import kotlinx.serialization.Serializable

@Serializable
data class LaunchProfileManifest(
    val id: String,
    val profileName: String,
    val baseId: String,
    val runtimeId: String,
    val driverId: String? = null,
    val environmentVariables: Map<String, String> = emptyMap(),
    val launchArgs: List<String> = emptyList(),
    val metadata: LaunchProfileMetadata = LaunchProfileMetadata(vkd3dHud = null),
) {
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        if (id.isBlank()) errors.add("LaunchProfileManifest.id cannot be blank")
        if (profileName.isBlank()) errors.add("LaunchProfileManifest.profileName cannot be blank")
        if (baseId.isBlank()) errors.add("LaunchProfileManifest.baseId cannot be blank")
        if (runtimeId.isBlank()) errors.add("LaunchProfileManifest.runtimeId cannot be blank")
        return errors
    }

    fun isValid(): Boolean = validate().isEmpty()

    fun validateReferences(bases: Set<String>, runtimes: Set<String>, drivers: Set<String>): List<String> {
        val errors = mutableListOf<String>()
        if (!bases.contains(baseId)) errors.add("baseId '$baseId' not found")
        if (!runtimes.contains(runtimeId)) errors.add("runtimeId '$runtimeId' not found")
        driverId?.let { did ->
            if (!drivers.contains(did)) errors.add("driverId '$did' not found")
        }
        return errors
    }
}

@Serializable
data class LaunchProfileMetadata(
    val esyncEnabled: Boolean = false,
    val fsyncEnabled: Boolean = false,
    val dxvkHud: String? = null,
    val vkd3dHud: String? = null,
    val description: String? = null,
    val author: String? = null,
)
