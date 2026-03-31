package app.gamegrub.ui.model

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.gamegrub.service.steam.domain.SteamInstallDomain
import app.gamegrub.service.steam.SteamService
import app.gamegrub.utils.container.ContainerManager
import app.gamegrub.utils.container.ContainerUtils
import app.gamegrub.enums.PathType
import app.gamegrub.enums.SyncResult
import app.gamegrub.utils.storage.MarkerUtils
import app.gamegrub.enums.Marker
import app.gamegrub.service.steam.SteamPaths
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
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
interface SteamAppScreenViewModelEntryPoint {
    fun steamAppScreenViewModel(): SteamAppScreenViewModel
}

fun getSteamAppScreenViewModel(context: Context): SteamAppScreenViewModel {
    return EntryPointAccessors
        .fromApplication(context.applicationContext, SteamAppScreenViewModelEntryPoint::class.java)
        .steamAppScreenViewModel()
}

/**
 * ViewModel for Steam game screen operations.
 * Replaces unmanaged IO scopes with lifecycle-aware coroutine handling.
 */
@HiltViewModel
class SteamAppScreenViewModel @Inject constructor(
    private val steamInstallDomain: SteamInstallDomain,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val TAG = "SteamAppScreenViewModel"
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    /**
     * Start download for a Steam app.
     */
    fun downloadApp(appId: Int, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch(ioDispatcher) {
            try {
                steamInstallDomain.downloadApp(appId)
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

    /**
     * Start download for a Steam app with specific DLC list.
     */
    fun downloadAppWithDlc(appId: Int, dlcAppIds: List<Int>, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch(ioDispatcher) {
            try {
                steamInstallDomain.downloadApp(appId, dlcAppIds, isUpdateOrVerify = false)
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

    /**
     * Sync cloud files for a Steam app.
     */
    fun syncUserCloudFiles(appId: Int, gameId: Int, onResult: (SyncResult) -> Unit) {
        viewModelScope.launch(ioDispatcher) {
            val accountId = SteamService.getSteam3AccountId()
            if (accountId == null) {
                withContext(Dispatchers.Main) {
                    onResult(SyncResult.Failed) // Use appropriate error result
                }
                return@launch
            }

            val containerManager = ContainerManager(context)
            val container = ContainerUtils.getOrCreateContainer(context, appId)
            containerManager.activateContainer(container)

            val prefixToPath: (String) -> String = { prefix ->
                PathType.from(prefix).toAbsPath(context, gameId, accountId)
            }
            val syncResult = SteamService.forceSyncUserFiles(
                appId = gameId,
                prefixToPath = prefixToPath,
            ).await()

            withContext(Dispatchers.Main) {
                onResult(syncResult.syncResult)
            }
        }
    }

    /**
     * Verify/update an installed app and clean up markers.
     */
    fun verifyAppWithCleanup(appId: Int, onComplete: () -> Unit) {
        viewModelScope.launch(ioDispatcher) {
            val container = ContainerUtils.getOrCreateContainer(context, appId)
            steamInstallDomain.downloadApp(appId)
            MarkerUtils.removeMarker(app.gamegrub.service.steam.SteamPaths.getAppDirPath(appId), Marker.STEAM_DLL_REPLACED)
            MarkerUtils.removeMarker(app.gamegrub.service.steam.SteamPaths.getAppDirPath(appId), Marker.STEAM_DLL_RESTORED)
            MarkerUtils.removeMarker(app.gamegrub.service.steam.SteamPaths.getAppDirPath(appId), Marker.STEAM_COLDCLIENT_USED)
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    /**
     * Cancel ongoing download for a Steam app.
     */
    fun cancelDownload(appId: Int, onComplete: () -> Unit) {
        viewModelScope.launch(ioDispatcher) {
            val downloadInfo = steamInstallDomain.getAppDownloadInfo(appId)
            downloadInfo?.cancel()
            steamInstallDomain.removeDownloadJob(appId)
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    /**
     * Verify or update an installed app.
     */
    fun verifyApp(appId: Int, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch(ioDispatcher) {
            try {
                steamInstallDomain.downloadApp(appId)
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

    /**
     * Cancel any pending operation for a Steam app.
     */
    fun cancelOperation(appId: Int, onComplete: () -> Unit) {
        viewModelScope.launch(ioDispatcher) {
            val downloadInfo = steamInstallDomain.getAppDownloadInfo(appId)
            downloadInfo?.cancel()
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    /**
     * Delete/uninstall a Steam app.
     */
    fun deleteApp(appId: Int, onComplete: () -> Unit) {
        viewModelScope.launch(ioDispatcher) {
            app.gamegrub.service.steam.SteamService.deleteApp(appId)
            app.gamegrub.GameGrubApp.events.emit(
                app.gamegrub.events.AndroidEvent.LibraryInstallStatusChanged(appId),
            )
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }
}
