package app.gamegrub.ui.model

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.gamegrub.service.gog.GOGConstants
import app.gamegrub.service.gog.domain.GOGInstallDomain
import app.gamegrub.utils.storage.StorageUtils
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@EntryPoint
@InstallIn(SingletonComponent::class)
interface GOGAppScreenViewModelEntryPoint {
    fun gogAppScreenViewModel(): GOGAppScreenViewModel
}

fun getGOGAppScreenViewModel(context: Context): GOGAppScreenViewModel {
    return EntryPointAccessors
        .fromApplication(context.applicationContext, GOGAppScreenViewModelEntryPoint::class.java)
        .gogAppScreenViewModel()
}

/**
 * ViewModel for GOG game screen operations.
 * Replaces unmanaged IO scopes with lifecycle-aware coroutine handling.
 */
@HiltViewModel
class GOGAppScreenViewModel @Inject constructor(
    private val gogInstallDomain: GOGInstallDomain,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val TAG = "GOGAppScreenViewModel"
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    fun showInstallConfirmationDialog(
        gameId: String,
        onDialogReady: (InstallDialogData) -> Unit,
    ) {
        viewModelScope.launch(ioDispatcher) {
            try {
                val game = gogInstallDomain.getGOGGameOf(gameId)
                val downloadSize = game?.downloadSize ?: 0L
                val availableSpace = StorageUtils.getAvailableSpace(GOGConstants.defaultGOGGamesPath)
                onDialogReady(
                    InstallDialogData(
                        gameId = gameId,
                        downloadSize = downloadSize,
                        availableSpace = availableSpace,
                    ),
                )
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to fetch game data for install dialog")
            }
        }
    }

    fun downloadGame(
        gameId: String,
        installPath: String,
        language: String,
        onResult: (Result<Unit>) -> Unit,
    ) {
        viewModelScope.launch(ioDispatcher) {
            try {
                val result = gogInstallDomain.downloadGame(gameId, installPath, language)
                withContext(Dispatchers.Main) {
                    result.fold(
                        onSuccess = { onResult(Result.success(Unit)) },
                        onFailure = { onResult(Result.failure(it)) },
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(Result.failure(e))
                }
            }
        }
    }

    fun cancelDownload(gameId: String, onComplete: () -> Unit) {
        viewModelScope.launch(ioDispatcher) {
            gogInstallDomain.cancelDownload(gameId)
            gogInstallDomain.cleanupDownload(gameId)
            onComplete()
        }
    }

    /**
     * Cancel download, cleanup, and optionally uninstall if game not installed after cancel.
     * Returns a result indicating whether download was cancelled and whether uninstall succeeded.
     */
    fun cancelDownloadAndMaybeUninstall(
        context: Context,
        gameId: String,
        libraryItem: app.gamegrub.data.LibraryItem,
        onResult: (CancelDownloadResult) -> Unit,
    ) {
        viewModelScope.launch(ioDispatcher) {
            val downloadInfo = gogInstallDomain.getDownloadInfo(gameId)
            val wasDownloading = downloadInfo != null &&
                downloadInfo.isActive() &&
                (downloadInfo.getProgress() ?: 0f) < 1f
            downloadInfo?.cancel()
            downloadInfo?.awaitCompletion()
            gogInstallDomain.cleanupDownload(gameId)

            val isInstalledAfterCancel = gogInstallDomain.isGameInstalled(gameId)
            if (isInstalledAfterCancel) {
                // Download completed and game ended up installed; don't show "Download cancelled"
                withContext(Dispatchers.Main) {
                    onResult(CancelDownloadResult.WasAlreadyInstalled)
                }
                return@launch
            }

            val result = gogInstallDomain.deleteGame(context, libraryItem)
            withContext(Dispatchers.Main) {
                onResult(
                    CancelDownloadResult.Success(
                        wasDownloading = wasDownloading,
                        uninstallResult = result,
                    ),
                )
            }
        }
    }

    fun uninstallGame(
        context: Context,
        libraryItem: app.gamegrub.data.LibraryItem,
        onResult: (Result<Unit>) -> Unit,
    ) {
        viewModelScope.launch(ioDispatcher) {
            try {
                val result = gogInstallDomain.deleteGame(context, libraryItem)
                withContext(Dispatchers.Main) {
                    result.fold(
                        onSuccess = { onResult(Result.success(Unit)) },
                        onFailure = { onResult(Result.failure(it)) },
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(Result.failure(e))
                }
            }
        }
    }

    fun updateContainerConfig(
        gameId: String,
        installPath: String,
        newLanguage: String,
        oldLanguage: String,
        onResult: (Result<Unit>) -> Unit,
    ) {
        if (oldLanguage == newLanguage) return
        viewModelScope.launch(ioDispatcher) {
            try {
                if (gogInstallDomain.isGameInstalled(gameId) &&
                    gogInstallDomain.getDownloadInfo(gameId)?.isActive() != true
                ) {
                    gogInstallDomain.downloadGame(gameId, installPath, newLanguage)
                }
                withContext(Dispatchers.Main) {
                    onResult(Result.success(Unit))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(Result.failure(e))
                }
            }
        }
    }
}

data class InstallDialogData(
    val gameId: String,
    val downloadSize: Long,
    val availableSpace: Long,
)

sealed class CancelDownloadResult {
    data object WasAlreadyInstalled : CancelDownloadResult()
    data class Success(
        val wasDownloading: Boolean,
        val uninstallResult: Result<Unit>,
    ) : CancelDownloadResult()
}