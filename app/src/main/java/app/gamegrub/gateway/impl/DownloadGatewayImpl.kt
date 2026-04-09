package app.gamegrub.gateway.impl

import app.gamegrub.data.DownloadInfo
import app.gamegrub.data.LibraryItem
import app.gamegrub.gateway.DownloadGateway
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

@Singleton
class DownloadGatewayImpl @Inject constructor() : DownloadGateway {

    private val activeDownloads = mutableMapOf<String, Flow<Float>>()
    private val downloadInfoFlows = mutableMapOf<String, MutableStateFlow<DownloadInfo?>>()

    override suspend fun startDownload(libraryItem: LibraryItem): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun pauseDownload(gameId: String): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun resumeDownload(gameId: String): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun cancelDownload(gameId: String): Result<Unit> {
        activeDownloads.remove(gameId)
        downloadInfoFlows.remove(gameId)
        return Result.success(Unit)
    }

    override fun getDownloadProgress(gameId: String): Flow<Float> {
        return activeDownloads[gameId] ?: MutableStateFlow(0f)
    }

    override fun getDownloadInfo(gameId: String): Flow<DownloadInfo?> {
        return downloadInfoFlows.getOrPut(gameId) { MutableStateFlow(null) }
    }

    override fun isDownloading(gameId: String): Boolean {
        return activeDownloads.containsKey(gameId)
    }

    override fun getActiveDownloads(): List<String> {
        return activeDownloads.keys.toList()
    }
}
