package app.gamegrub.runtime.manifest

import kotlinx.serialization.Serializable

@Serializable
data class BaseManifest(
    val id: String,
    val version: String,
    val contentHash: String,
    val createdAt: Long = System.currentTimeMillis(),
    val rootfsPath: String,
    val description: String = "",
) {
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        if (id.isBlank()) errors.add("BaseManifest.id cannot be blank")
        if (version.isBlank()) errors.add("BaseManifest.version cannot be blank")
        if (contentHash.isBlank()) {
            errors.add("BaseManifest.contentHash cannot be blank")
        } else if (contentHash.length != 64) {
            errors.add("BaseManifest.contentHash must be SHA256 (64 hex chars)")
        }
        if (rootfsPath.isBlank()) errors.add("BaseManifest.rootfsPath cannot be blank")
        return errors
    }

    fun isValid(): Boolean = validate().isEmpty()

    companion object {
        fun isValidSha256(hash: String): Boolean {
            return hash.length == 64 && hash.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
        }
    }
}

@Serializable
enum class RuntimeType {
    WINE,
    PROTON,
    TRANSLATOR,
}

@Serializable
data class RuntimeManifest(
    val id: String,
    val version: String,
    val contentHash: String,
    val createdAt: Long = System.currentTimeMillis(),
    val runtimePath: String,
    val baseId: String,
    val runtimeType: RuntimeType,
    val metadata: RuntimeMetadata = RuntimeMetadata(),
) {
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        if (id.isBlank()) errors.add("RuntimeManifest.id cannot be blank")
        if (version.isBlank()) errors.add("RuntimeManifest.version cannot be blank")
        if (contentHash.isBlank()) errors.add("RuntimeManifest.contentHash cannot be blank")
        if (contentHash.length != 64) errors.add("RuntimeManifest.contentHash must be SHA256 (64 hex chars)")
        if (runtimePath.isBlank()) errors.add("RuntimeManifest.runtimePath cannot be blank")
        if (baseId.isBlank()) errors.add("RuntimeManifest.baseId cannot be blank")
        return errors
    }

    fun isValid(): Boolean = validate().isEmpty()
}

@Serializable
data class RuntimeMetadata(
    val translatorVersion: String? = null,
    val dxvkVersion: String? = null,
    val vkd3dVersion: String? = null,
    val wineBuild: String? = null,
    val protonVersion: String? = null,
)
