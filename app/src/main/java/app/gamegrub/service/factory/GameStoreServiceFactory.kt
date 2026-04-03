package app.gamegrub.service.factory

import app.gamegrub.data.GameSource
import app.gamegrub.service.auth.GameStoreAuth
import app.gamegrub.service.cloud.GameStoreCloudSaves
import app.gamegrub.service.download.GameStoreDownloader

interface GameStoreServiceFactory {
    fun getAuthService(source: GameSource): GameStoreAuth?

    fun getDownloadService(source: GameSource): GameStoreDownloader?

    fun getCloudSavesService(source: GameSource): GameStoreCloudSaves?

    fun getAllAuthServices(): List<GameStoreAuth>

    fun getAllDownloadServices(): List<GameStoreDownloader>

    fun getAllCloudSavesServices(): List<GameStoreCloudSaves>
}
