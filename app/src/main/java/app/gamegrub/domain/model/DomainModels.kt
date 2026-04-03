package app.gamegrub.domain.model

import app.gamegrub.data.GameSource

data class Game(
    val id: Int,
    val appId: String,
    val name: String,
    val gameSource: GameSource,
    val iconUrl: String,
    val headerUrl: String,
    val isInstalled: Boolean,
    val installPath: String,
    val installSize: Long,
    val downloadSize: Long,
    val lastPlayed: Long,
    val playTime: Long,
    val developer: String,
    val publisher: String,
    val releaseDate: String,
    val description: String,
)

data class GameSourceInfo(
    val source: GameSource,
    val displayName: String,
    val isLoggedIn: Boolean,
    val gameCount: Int,
    val installedCount: Int,
)

data class LibraryStats(
    val totalGames: Int,
    val installedGames: Int,
    val totalSize: Long,
    val totalPlayTime: Long,
    val bySource: Map<GameSource, GameSourceInfo>,
)
