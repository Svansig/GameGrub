package app.gamegrub.service.steam.domain

import android.content.Context
import app.gamegrub.data.PostSyncInfo
import app.gamegrub.enums.SaveLocation
import app.gamegrub.service.steam.managers.SteamAchievementManager
import app.gamegrub.service.steam.managers.SteamCloudSavesManager
import app.gamegrub.statsgen.Achievement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SteamCloudStatsDomain @Inject constructor(
    val cloudSavesManager: SteamCloudSavesManager,
    val achievementManager: SteamAchievementManager,
) {
    fun forceSyncUserFiles(
        appId: Int,
        prefixToPath: (String) -> String,
        preferredSave: SaveLocation = SaveLocation.None,
        parentScope: CoroutineScope = CoroutineScope(kotlinx.coroutines.Dispatchers.IO),
        overrideLocalChangeNumber: Long? = null,
    ): Deferred<PostSyncInfo> = cloudSavesManager.forceSyncUserFiles(
        appId = appId,
        prefixToPath = prefixToPath,
        preferredSave = preferredSave,
        parentScope = parentScope,
        overrideLocalChangeNumber = overrideLocalChangeNumber,
    )

    val cachedAchievements: StateFlow<List<Achievement>?> = achievementManager.cachedAchievements
    val cachedAchievementsAppId: StateFlow<Int?> = achievementManager.cachedAchievementsAppId

    fun clearCachedAchievements() = achievementManager.clearCachedAchievements()

    suspend fun generateAchievements(appId: Int, configDirectory: String) =
        achievementManager.generateAchievements(appId, configDirectory)

    fun getGseSaveDirs(context: Context, appId: Int): List<File> =
        achievementManager.getGseSaveDirs(context, appId)

    suspend fun syncAchievementsFromGoldberg(context: Context, appId: Int) =
        achievementManager.syncAchievementsFromGoldberg(context, appId)

    suspend fun storeAchievementUnlocks(
        appId: Int,
        configDirectory: String,
        unlockedNames: Set<String>,
        gseStatsDir: File,
    ) = achievementManager.storeAchievementUnlocks(appId, configDirectory, unlockedNames, gseStatsDir)
}