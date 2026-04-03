package app.gamegrub.gateway

import app.gamegrub.data.DownloadInfo
import app.gamegrub.data.GameSource
import app.gamegrub.data.LibraryItem
import kotlinx.coroutines.flow.Flow

sealed class LaunchState {
    data object Idle : LaunchState()
    data class Preparing(val message: String) : LaunchState()
    data object Launching : LaunchState()
    data class Error(val message: String) : LaunchState()
}

interface LaunchGateway {
    suspend fun launchGame(libraryItem: LibraryItem): Result<Unit>

    suspend fun prepareLaunch(libraryItem: LibraryItem): Result<Unit>

    fun getLaunchState(appId: String): LaunchState

    fun cancelLaunch(appId: String)

    fun getActiveLaunchCount(): Int
}

interface DownloadGateway {
    suspend fun startDownload(libraryItem: LibraryItem): Result<Unit>

    suspend fun pauseDownload(gameId: String): Result<Unit>

    suspend fun resumeDownload(gameId: String): Result<Unit>

    suspend fun cancelDownload(gameId: String): Result<Unit>

    fun getDownloadProgress(gameId: String): Flow<Float>

    fun getDownloadInfo(gameId: String): Flow<DownloadInfo?>

    fun isDownloading(gameId: String): Boolean

    fun getActiveDownloads(): List<String>
}
