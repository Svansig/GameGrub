package app.gamegrub.gamefixes

import app.gamegrub.data.GameSource

/**
 * Bound by Flame (GOG)
 */
val GOG_Fix_1787707874: KeyedGameFix = KeyedLaunchArgFix(
    gameSource = GameSource.GOG,
    gameId = "1787707874",
    launchArgs = "-lang=eng",
)
