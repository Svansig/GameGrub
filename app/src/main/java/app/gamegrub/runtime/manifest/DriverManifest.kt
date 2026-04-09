package app.gamegrub.runtime.manifest

import kotlinx.serialization.Serializable

@Serializable
enum class DriverType {
    VULKAN,
    GLES,
    TURNIP,
    ADRENO,
}

@Serializable
data class DriverManifest(
    val id: String,
    val version: String,
    val contentHash: String,
    val createdAt: Long = System.currentTimeMillis(),
    val driverPath: String,
    val driverType: DriverType,
    val minGlibcVersion: String? = null,
) {
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        if (id.isBlank()) errors.add("DriverManifest.id cannot be blank")
        if (version.isBlank()) errors.add("DriverManifest.version cannot be blank")
        if (contentHash.isBlank()) errors.add("DriverManifest.contentHash cannot be blank")
        if (contentHash.length != 64) errors.add("DriverManifest.contentHash must be SHA256 (64 hex chars)")
        if (driverPath.isBlank()) errors.add("DriverManifest.driverPath cannot be blank")
        return errors
    }

    fun isValid(): Boolean = validate().isEmpty()
}
