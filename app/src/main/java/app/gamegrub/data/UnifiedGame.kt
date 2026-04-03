package app.gamegrub.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import app.gamegrub.enums.AppType

/**
 * Unified Game entity for Room database.
 * Represents a game from any supported store (Steam, GOG, Epic, Amazon).
 *
 * Store-specific data is stored as JSON in the storeData field.
 */
@Entity(tableName = "games")
data class UnifiedGame(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,

    /**
     * Unique app ID within GameNative.
     * Format: "{STORE}_{storeId}" (e.g., "STEAM_12345", "GOG_abc123", "EPIC_456", "AMAZON_amzn1.adg.product.XXXX")
     */
    @ColumnInfo(name = "app_id")
    val appId: String,

    /** Store identifier */
    @ColumnInfo(name = "game_source")
    val gameSource: GameSource,

    // Common fields
    @ColumnInfo(name = "name")
    val name: String = "",

    @ColumnInfo(name = "icon_url")
    val iconUrl: String = "",

    @ColumnInfo(name = "header_url")
    val headerUrl: String = "",

    @ColumnInfo(name = "is_installed")
    val isInstalled: Boolean = false,

    @ColumnInfo(name = "install_path")
    val installPath: String = "",

    @ColumnInfo(name = "install_size")
    val installSize: Long = 0,

    @ColumnInfo(name = "download_size")
    val downloadSize: Long = 0,

    @ColumnInfo(name = "last_played")
    val lastPlayed: Long = 0,

    @ColumnInfo(name = "play_time")
    val playTime: Long = 0,

    @ColumnInfo(name = "type")
    val type: AppType = AppType.game,

    @ColumnInfo(name = "description")
    val description: String = "",

    @ColumnInfo(name = "developer")
    val developer: String = "",

    @ColumnInfo(name = "publisher")
    val publisher: String = "",

    @ColumnInfo(name = "release_date")
    val releaseDate: String = "",

    /**
     * Store-specific data as JSON string.
     * - Steam: depots, branches, packageId, licenseFlags, config, ufs
     * - GOG: slug, genres, languages, exclude
     * - Epic: catalogId, appName, namespace, platform, version, executable, cloudSaveEnabled, isDLC, eos fields
     * - Amazon: productId, entitlementId, versionId, productSku, productJson
     */
    @ColumnInfo(name = "store_data")
    val storeData: String = "",
)
