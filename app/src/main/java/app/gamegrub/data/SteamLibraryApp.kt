package app.gamegrub.data

import androidx.room.ColumnInfo
import app.gamegrub.enums.AppType
import app.gamegrub.enums.Language

/**
 * Lightweight Steam app projection used by the library list to avoid loading oversized app payloads.
 */
data class SteamLibraryApp(
    @ColumnInfo("id")
    val id: Int,
    @ColumnInfo("owner_account_id")
    val ownerAccountId: List<Int> = emptyList(),
    @ColumnInfo("name")
    val name: String = "",
    @ColumnInfo("type")
    val type: AppType = AppType.invalid,
    @ColumnInfo("client_icon_hash")
    val clientIconHash: String = "",
    @ColumnInfo("library_assets")
    val libraryAssets: LibraryAssetsInfo = LibraryAssetsInfo(),
    @ColumnInfo("header_image")
    val headerImage: Map<Language, String> = emptyMap(),
    @ColumnInfo("install_dir")
    val installDir: String = "",
) {
    val headerUrl: String
        get() = "https://shared.steamstatic.com/store_item_assets/steam/apps/$id/header.jpg"

    private fun getFallbackUrl(images: Map<Language, String>, language: Language): String? {
        return if (images.contains(language)) {
            images[language]
        } else if (images.isNotEmpty()) {
            images.values.first()
        } else if (headerImage.contains(language)) {
            headerImage[language]
        } else if (headerImage.isNotEmpty()) {
            headerImage.values.first()
        } else {
            ""
        }
    }

    fun getCapsuleUrl(language: Language = Language.english, large: Boolean = false): String {
        val capsules = if (large) {
            libraryAssets.libraryCapsule.image2x
        } else {
            libraryAssets.libraryCapsule.image
        }

        val imageLink = this.getFallbackUrl(capsules, language)
        return if (imageLink.isNullOrEmpty()) "" else "https://shared.steamstatic.com/store_item_assets/steam/apps/$id/$imageLink"
    }

    fun getHeroUrl(language: Language = Language.english, large: Boolean = false): String {
        val images = if (large) {
            libraryAssets.libraryHero.image2x
        } else {
            libraryAssets.libraryHero.image
        }

        val imageLink = this.getFallbackUrl(images, language)
        return if (imageLink.isNullOrEmpty()) "" else "https://shared.steamstatic.com/store_item_assets/steam/apps/$id/$imageLink"
    }
}
