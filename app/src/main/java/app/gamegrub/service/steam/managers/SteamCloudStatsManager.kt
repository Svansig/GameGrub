package app.gamegrub.service.steam.managers

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cloud/stats domain manager: cloud save sync and achievement/stat workflows.
 */
@Singleton
class SteamCloudStatsManager @Inject constructor(
    val cloudSavesManager: SteamCloudSavesManager,
    val achievementManager: SteamAchievementManager,
)

