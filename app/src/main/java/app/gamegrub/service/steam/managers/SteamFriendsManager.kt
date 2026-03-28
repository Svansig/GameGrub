package app.gamegrub.service.steam.managers

import app.gamegrub.service.steam.di.GameEventEmitter
import app.gamegrub.service.steam.di.SteamConnection
import app.gamegrub.service.steam.di.SteamPreferences
import app.gamegrub.service.steam.di.SteamUserClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SteamFriendsManager @Inject constructor(
    private val userClient: SteamUserClient,
    private val connection: SteamConnection,
    private val preferences: SteamPreferences,
    private val eventEmitter: GameEventEmitter,
) {
    val personaState: StateFlow<app.gamegrub.service.steam.di.SteamPersona>
        get() = userClient.personaState

    suspend fun setPersonaState(state: Int) = withContext(Dispatchers.IO) {
        try {
            userClient.setPersonaState(state)
            preferences.personaState = state
        } catch (e: Exception) {
            Timber.e(e, "Failed to set persona state")
        }
    }

    suspend fun requestUserPersona() = withContext(Dispatchers.IO) {
        val steamId = connection.steamId
        if (steamId != null) {
            userClient.requestPersonaInfo(steamId)
        } else {
            Timber.w("Cannot request persona - no Steam ID")
        }
    }

    suspend fun kickPlayingSession(onlyGame: Boolean = true): Boolean = withContext(Dispatchers.IO) {
        try {
            userClient.kickPlayingSession(onlyGame)
        } catch (e: Exception) {
            Timber.e(e, "Failed to kick playing session")
            false
        }
    }
}
