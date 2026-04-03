package app.gamegrub.service.download

import app.gamegrub.data.DownloadInfo
import app.gamegrub.data.GameSource

interface GameStoreDownloader {
    val gameSource: GameSource

    suspend fun download(gameId: String): Result<Unit>

    suspend fun pause(gameId: String): Result<Unit>

    suspend fun resume(gameId: String): Result<Unit>

    suspend fun cancel(gameId: String): Result<Unit>

    fun getProgress(gameId: String): Float

    suspend fun getDownloadInfo(gameId: String): DownloadInfo?

    fun isDownloading(gameId: String): Boolean
}
