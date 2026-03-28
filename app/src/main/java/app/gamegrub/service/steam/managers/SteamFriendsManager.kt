package app.gamegrub.service.steam.managers

import app.gamegrub.PrefManager
import app.gamegrub.data.SteamFriend
import app.gamegrub.events.SteamEvent
import app.gamegrub.service.steam.di.GameEventEmitter
import app.gamegrub.service.steam.di.SteamConnection
import app.gamegrub.service.steam.di.SteamPersona
import app.gamegrub.service.steam.di.SteamPreferences
import app.gamegrub.service.steam.di.SteamUserClient
import `in`.dragonbra.javasteam.enums.EPersonaState
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

    val personaState: StateFlow<SteamPersona> get() = userClient.personaState

    suspend fun setPersonaState(state: EPersonaState) = withContext(Dispatchers.IO) {
        try {
            userClient.setPersonaState(state.code())
            preferences.personaState = state.code()
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

    suspend fun getSelfCurrentlyPlayingAppId(): Int? = withContext(Dispatchers.IO) {
        val persona = userClient.personaState.value
        if (persona.isPlayingGame) persona.gameAppID else null
    }

    suspend fun kickPlayingSession(onlyGame: Boolean = true): Boolean = withContext(Dispatchers.IO) {
        try {
            userClient.kickPlayingSession(onlyGame)
        } catch (e: Exception) {
            Timber.e(e, "Failed to kick playing session")
            false
        }
    }

    fun handlePersonaStateUpdate(
        friendSteamId: Long,
        personaName: String?,
        avatarHash: String?,
        gamePlayedID: Int?,
        gamePlayedName: String?,
        personaState: Int?,
        friendRelationship: Int?,
    ) {
        val localSteamId = connection.steamId?.convertToUInt64()

        if (friendSteamId == localSteamId) {
            val name = personaName ?: PrefManager.steamUserName
            PrefManager.steamUserName = name
            if (avatarHash != null) PrefManager.steamUserAvatarHash = avatarHash
            if (gamePlayedID != null && gamePlayedID > 0) {
                PrefManager.steamUserAccountId = `in`.dragonbra.javasteam.types.SteamID(friendSteamId).accountID.toInt()
            }
        } else {
            eventEmitter.emitSteamEvent(
                SteamEvent.PersonaStateReceived(
                    persona = SteamFriend(
                        avatarHash = avatarHash ?: "",
                        gameAppID = gamePlayedID ?: 0,
                        gameName = gamePlayedName ?: "",
                        name = personaName ?: "",
                        state = EPersonaState.from(personaState ?: EPersonaState.Offline.code()),
                    ),
                ),
            )
        }
    }
}
