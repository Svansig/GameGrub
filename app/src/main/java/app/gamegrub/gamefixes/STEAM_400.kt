package app.gamegrub.gamefixes

import app.gamegrub.data.GameSource

/**
 * Portal (Steam)
 */
val STEAM_Fix_400: KeyedGameFix = KeyedLaunchArgFix(
    gameSource = GameSource.STEAM,
    gameId = "400",
    launchArgs = "-game portal",
)
