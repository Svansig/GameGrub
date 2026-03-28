package app.gamegrub.service.steam.managers

import app.gamegrub.PrefManager
import app.gamegrub.data.SteamFriend
import app.gamegrub.events.AndroidEvent
import app.gamegrub.GameGrubApp
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.SteamFriends
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.PersonaStateCallback
import `in`.dragonbra.javasteam.types.SteamID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class SteamFriendsManager(
    private val getService: () -> SteamServiceAccess?,
) {

    private val _localPersona = kotlinx.coroutines.flow.MutableStateFlow(
        SteamFriend(name = PrefManager.steamUserName, avatarHash = PrefManager.steamUserAvatarHash),
    )
    val localPersona = _localPersona.asStateFlow()

    suspend fun setPersonaState(state: EPersonaState) = withContext(Dispatchers.IO) {
        PrefManager.personaState = state
        getService()?.steamFriends?.setPersonaState(state)
    }

    suspend fun requestUserPersona() = withContext(Dispatchers.IO) {
        val userSteamId = getService()?.steamClient?.steamID
        if (userSteamId != null) {
            getService()?.steamFriends?.requestFriendInfo(userSteamId)
        }
    }

    suspend fun getSelfCurrentlyPlayingAppId(): Int? = withContext(Dispatchers.IO) {
        val self = _localPersona.value
        if (self.isPlayingGame) self.gameAppID else null
    }

    suspend fun kickPlayingSession(onlyGame: Boolean = true): Boolean = withContext(Dispatchers.IO) {
        val steamUser = getService()?.steamUser ?: return@withContext false
        try {
            val isPlayingBlocked = kotlinx.coroutines.flow.MutableStateFlow(false)
            steamUser.kickPlayingSession(onlyStopGame = onlyGame)

            val deadline = System.currentTimeMillis() + 5000
            while (System.currentTimeMillis() < deadline) {
                if (!isPlayingBlocked.value) return@withContext true
                kotlinx.coroutines.delay(100)
            }
            false
        } catch (_: Exception) {
            false
        }
    }

    fun handlePersonaState(callback: PersonaStateCallback) {
        val friendSteamId = callback.friendID ?: return
        val localSteamId = getService()?.steamClient?.steamID

        if (friendSteamId == localSteamId) {
            val name = callback.personaName ?: PrefManager.steamUserName
            val avatarHash = callback.avatarHash
            val profileState = callback.profileState
            val gameState = callback.gamePlayedID
            val gameName = callback.gamePlayedName

            _localPersona.value = SteamFriend(
                name = name,
                avatarHash = avatarHash ?: "",
                profileState = profileState,
                gameAppID = gameState ?: 0,
                gameName = gameName,
            )

            PrefManager.steamUserName = name
            if (avatarHash != null) {
                PrefManager.steamUserAvatarHash = avatarHash
            }
            if (gameState != null && gameState > 0) {
                PrefManager.steamUserAccountId = friendSteamId.accountID.toInt()
            }

            Timber.d("Updated local persona: $name")
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
