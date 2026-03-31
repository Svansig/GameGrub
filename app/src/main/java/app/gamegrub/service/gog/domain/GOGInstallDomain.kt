package app.gamegrub.service.gog.domain

import android.content.Context
import app.gamegrub.data.DownloadInfo
import app.gamegrub.data.GOGGame
import app.gamegrub.data.LibraryItem
import app.gamegrub.service.gog.GOGService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@EntryPoint
@InstallIn(SingletonComponent::class)
interface GOGInstallDomainEntryPoint {
    fun gogInstallDomain(): GOGInstallDomain
}

fun getGOGInstallDomain(context: Context): GOGInstallDomain {
    return EntryPointAccessors
        .fromApplication(context.applicationContext, GOGInstallDomainEntryPoint::class.java)
        .gogInstallDomain()
}

/**
 * Domain for GOG install/download/uninstall operations.
 * Provides a clean interface for UI layer (e.g., GOGAppScreen) to orchestrate
 * game installation workflows without direct service calls.
 */
@Singleton
class GOGInstallDomain @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val TAG = "GOGInstallDomain"

    fun getDownloadInfo(gameId: String): DownloadInfo? = GOGService.getDownloadInfo(gameId)

    fun downloadGame(
        gameId: String,
        installPath: String,
        containerLanguage: String,
    ): Result<DownloadInfo?> {
        Timber.tag(TAG).d("downloadGame: gameId=$gameId, installPath=$installPath, language=$containerLanguage")
        return GOGService.downloadGame(context, gameId, installPath, containerLanguage)
    }

    fun cancelDownload(gameId: String): Boolean {
        Timber.tag(TAG).i("cancelDownload: gameId=$gameId")
        return GOGService.cancelDownload(gameId)
    }

    fun cleanupDownload(gameId: String) {
        Timber.tag(TAG).d("cleanupDownload: gameId=$gameId")
        GOGService.cleanupDownload(gameId)
    }

    fun isGameInstalled(gameId: String): Boolean = GOGService.isGameInstalled(gameId)

    fun getInstallPath(gameId: String): String? = GOGService.getInstallPath(gameId)

    fun getGOGGameOf(gameId: String): GOGGame? = GOGService.getGOGGameOf(gameId)

    suspend fun deleteGame(context: Context, libraryItem: LibraryItem): Result<Unit> {
        Timber.tag(TAG).i("deleteGame: appId=${libraryItem.appId}")
        return GOGService.deleteGame(context, libraryItem)
    }

    fun getCurrentlyDownloadingGame(): String? = GOGService.getCurrentlyDownloadingGame()

    fun hasActiveDownload(): Boolean = GOGService.hasActiveDownload()
}