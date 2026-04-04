package app.gamegrub.content.manifest

import kotlinx.serialization.Serializable

/**
 * Represents a single entry in the component manifest.
 *
 * Manifest entries describe downloadable components like drivers (DXVK, VKD3D,
 * Wine, Proton, etc.) with their download URLs and variant information.
 *
 * @param id Unique identifier for this component version (e.g., "dxvk-2.1")
 * @param name Human-readable display name
 * @param url Download URL for the component archive
 * @param variant Optional variant identifier (e.g., "glibc", "bionic")
 * @param arch Optional architecture filter (e.g., "a64", "a64v8")
 */
@Serializable
data class ManifestEntry(
    val id: String,
    val name: String,
    val url: String,
    val variant: String? = null,
    val arch: String? = null,
)

/**
 * Root container for parsed manifest data.
 *
 * @param version Manifest schema version
 * @param updatedAt ISO-8601 timestamp of last manifest update
 * @param items Map of content-type to list of available entries
 */
@Serializable
data class ManifestData(
    val version: Int?,
    val updatedAt: String?,
    val items: Map<String, List<ManifestEntry>>,
) {
    companion object {
        /**
         * Creates an empty manifest with no entries.
         */
        fun empty(): ManifestData = ManifestData(null, null, emptyMap())
    }
}

/**
 * Defines known content type identifiers used in manifests.
 *
 * These mirror the ContentProfile.ContentType values from the Winlator
 * contents system and are used to categorize manifest entries.
 */
object ManifestContentTypes {
    const val DRIVER = "driver"
    const val DXVK = "dxvk"
    const val VKD3D = "vkd3d"
    const val BOX64 = "box64"
    const val WOWBOX64 = "wowbox64"
    const val FEXCORE = "fexcore"
    const val WINE = "wine"
    const val PROTON = "proton"
}
