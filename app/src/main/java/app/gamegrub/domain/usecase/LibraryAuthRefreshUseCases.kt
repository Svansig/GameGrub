package app.gamegrub.domain.usecase

import android.content.Context
import app.gamegrub.data.GameSource
import app.gamegrub.gateway.AuthStateGateway
import app.gamegrub.gateway.LibraryGateway
import app.gamegrub.service.amazon.AmazonService
import app.gamegrub.service.epic.EpicService
import app.gamegrub.service.gog.GOGService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Refreshes library data using unified gateway boundaries and current auth state.
 */
class RefreshLibraryOrchestrationUseCase @Inject constructor(
    private val libraryGateway: LibraryGateway,
    private val authStateGateway: AuthStateGateway,
) {
    suspend operator fun invoke() {
        libraryGateway.refreshLibrary(GameSource.STEAM)

        if (authStateGateway.hasStoredCredentials(GameSource.GOG)) {
            libraryGateway.refreshLibrary(GameSource.GOG)
        }

        if (authStateGateway.hasStoredCredentials(GameSource.AMAZON)) {
            libraryGateway.refreshLibrary(GameSource.AMAZON)
        }
    }
}

/**
 * Completes OAuth login callback handling and kicks off the corresponding library sync.
 */
class CompleteLibraryOAuthUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    suspend operator fun invoke(source: GameSource, authCode: String): Result<Unit> {
        return when (source) {
            GameSource.GOG -> runCatching {
                val result = GOGService.authenticateWithCode(context, authCode)
                if (!result.isSuccess) {
                    throw result.exceptionOrNull() ?: IllegalStateException("GOG authentication failed")
                }
                GOGService.start(context)
                GOGService.triggerLibrarySync(context)
            }

            GameSource.EPIC -> runCatching {
                val result = EpicService.authenticateWithCode(context, authCode)
                if (!result.isSuccess) {
                    throw result.exceptionOrNull() ?: IllegalStateException("Epic authentication failed")
                }
                EpicService.start(context)
                EpicService.triggerLibrarySync(context)
            }

            GameSource.AMAZON -> runCatching {
                val result = AmazonService.authenticateWithCode(context, authCode)
                if (!result.isSuccess) {
                    throw result.exceptionOrNull() ?: IllegalStateException("Amazon authentication failed")
                }
                AmazonService.start(context)
                AmazonService.triggerLibrarySync(context)
            }

            GameSource.STEAM,
            GameSource.CUSTOM_GAME,
                -> Result.failure(IllegalArgumentException("OAuth completion is unsupported for source=$source"))
        }
    }
}

