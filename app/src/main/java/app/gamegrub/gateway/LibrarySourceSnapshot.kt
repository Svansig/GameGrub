package app.gamegrub.gateway

import app.gamegrub.data.AmazonGame
import app.gamegrub.data.EpicGame
import app.gamegrub.data.GOGGame
import app.gamegrub.data.SteamLibraryApp

/**
 * Unified snapshot of source-specific library rows consumed by the library presentation pipeline.
 */
data class LibrarySourceSnapshot(
    val steamApps: List<SteamLibraryApp> = emptyList(),
    val gogGames: List<GOGGame> = emptyList(),
    val epicGames: List<EpicGame> = emptyList(),
    val amazonGames: List<AmazonGame> = emptyList(),
    val downloadedSteamAppIds: Set<Int> = emptySet(),
)

