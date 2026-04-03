package app.gamegrub.data

data class GameDetails(
    val libraryItem: LibraryItem,
    val achievements: List<AchievementInfo> = emptyList(),
    val cloudSaves: CloudSaveInfo? = null,
    val dlcList: List<LibraryItem> = emptyList(),
    val updateInfo: UpdateInfo? = null,
)

data class AchievementInfo(
    val id: String,
    val name: String,
    val description: String,
    val iconUrl: String,
    val unlocked: Boolean,
    val unlockTime: Long? = null,
)

data class CloudSaveInfo(
    val lastSyncTime: Long,
    val size: Long,
    val fileCount: Int,
    val status: CloudSaveStatus,
)

enum class CloudSaveStatus {
    SYNCED,
    PENDING_UPLOAD,
    PENDING_DOWNLOAD,
    CONFLICT,
    ERROR,
}

data class UpdateInfo(
    val available: Boolean,
    val currentVersion: String,
    val newVersion: String?,
    val downloadSize: Long,
    val releaseNotes: String? = null,
)
