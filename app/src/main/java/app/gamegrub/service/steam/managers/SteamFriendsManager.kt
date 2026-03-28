package app.gamegrub.service.steam.managers

import app.gamegrub.GameGrubApp
import app.gamegrub.PrefManager
import app.gamegrub.data.SteamFriend
import app.gamegrub.events.AndroidEvent
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.PersonaStateCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SteamFriendsManager @Inject constructor() {

    private val _localPersona = MutableStateFlow(
        SteamFriend(name = PrefManager.steamUserName, avatarHash = PrefManager.steamUserAvatarHash),
    )
    val localPersona: StateFlow<SteamFriend> = _localPersona.asStateFlow()

    suspend fun setPersonaState(state: EPersonaState) = withContext(Dispatchers.IO) {
        PrefManager.personaState = state
        SteamService.instance?._steamFriends?.setPersonaState(state)
    }

    suspend fun requestUserPersona() = withContext(Dispatchers.IO) {
        val steamId = SteamService.instance?.steamClient?.steamID ?: return@withContext
        SteamService.instance?._steamFriends?.requestFriendInfo(steamId)
    }

    suspend fun getSelfCurrentlyPlayingAppId(): Int? = withContext(Dispatchers.IO) {
        val self = _localPersona.value
        if (self.isPlayingGame) self.gameAppID else null
    }

    suspend fun kickPlayingSession(onlyGame: Boolean = true): Boolean = withContext(Dispatchers.IO) {
        val steamUser = SteamService.instance?._steamUser ?: return@withContext false
        try {
            steamUser.kickPlayingSession(onlyStopGame = onlyGame)
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to kick playing session")
            false
        }
    }

    fun handlePersonaState(callback: PersonaStateCallback) {
        val friendSteamId = callback.friendID ?: return
        val localSteamId = SteamService.instance?.steamClient?.steamID

        if (friendSteamId == localSteamId) {
            val name = callback.personaName ?: PrefManager.steamUserName
            _localPersona.value = SteamFriend(
                name = name,
                avatarHash = callback.avatarHash ?: "",
                profileState = callback.profileState,
                gameAppID = callback.gamePlayedID ?: 0,
                gameName = callback.gamePlayedName,
            )
            PrefManager.steamUserName = name
            if (callback.avatarHash != null) PrefManager.steamUserAvatarHash = callback.avatarHash
            if (callback.gamePlayedID != null && callback.gamePlayedID > 0) {
                PrefManager.steamUserAccountId = friendSteamId.accountID.toInt()
            }
        } else {
            GameGrubApp.events.emit(
                AndroidEvent.PersonaStateReceived(
                    SteamID = friendSteamId.convertToUInt64(),
                    personaName = callback.personaName,
                    avatarHash = callback.avatarHash,
                    gamePlayedID = callback.gamePlayedID,
                    gamePlayedName = callback.gamePlayedName,
                    personaState = callback.personaState,
                    friendRelationship = callback.friendRelationship,
                ),
            )
        }
    }
}
