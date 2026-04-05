package app.gamegrub.gateway.impl

import android.content.Context
import app.gamegrub.data.GameSource
import app.gamegrub.gateway.AuthStateGateway
import app.gamegrub.service.amazon.AmazonService
import app.gamegrub.service.epic.EpicService
import app.gamegrub.service.gog.GOGService
import app.gamegrub.service.steam.SteamService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthStateGatewayImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : AuthStateGateway {

    override fun hasStoredCredentials(source: GameSource): Boolean {
        return when (source) {
            GameSource.STEAM -> SteamService.isLoggedIn
            GameSource.GOG -> GOGService.hasStoredCredentials(context)
            GameSource.EPIC -> EpicService.hasStoredCredentials(context)
            GameSource.AMAZON -> AmazonService.hasStoredCredentials(context)
            GameSource.CUSTOM_GAME -> false
        }
    }

    override fun isLoggedIn(source: GameSource): Boolean {
        return when (source) {
            GameSource.STEAM -> SteamService.isLoggedIn
            GameSource.GOG -> GOGService.hasStoredCredentials(context)
            GameSource.EPIC -> EpicService.hasStoredCredentials(context)
            GameSource.AMAZON -> AmazonService.hasStoredCredentials(context)
            GameSource.CUSTOM_GAME -> false
        }
    }

    override fun getLoggedInStores(): Set<GameSource> {
        val stores = mutableSetOf<GameSource>()
        if (SteamService.isLoggedIn) stores.add(GameSource.STEAM)
        if (GOGService.hasStoredCredentials(context)) stores.add(GameSource.GOG)
        if (EpicService.hasStoredCredentials(context)) stores.add(GameSource.EPIC)
        if (AmazonService.hasStoredCredentials(context)) stores.add(GameSource.AMAZON)
        return stores
    }
}
