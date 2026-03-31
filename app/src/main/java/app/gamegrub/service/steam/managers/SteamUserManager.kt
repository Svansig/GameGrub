package app.gamegrub.service.steam.managers

import app.gamegrub.PrefManager
import app.gamegrub.service.steam.di.SteamConnection
import `in`.dragonbra.javasteam.types.SteamID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SteamUserManager @Inject constructor(
    private val connection: SteamConnection,
) {
    fun removeSpecialChars(value: String): String = value.replace(Regex("[^\\u0000-\\u007F]"), "")

    fun getSteamId(): SteamID? = connection.steamId

    fun getSteamId64(): Long? {
        val activeSteamId = connection.steamId
        if (activeSteamId != null) {
            return activeSteamId.convertToUInt64()
        }
        return PrefManager.steamUserSteamId64.takeIf { it > 0L }
    }

    fun getSteam3AccountId(): Long? {
        val activeSteamId = connection.steamId
        if (activeSteamId != null) {
            return activeSteamId.accountID
        }
        return PrefManager.steamUserAccountId.toLong().takeIf { it > 0L }
    }

    fun getAccountIdInt(): Int? {
        val activeSteamId = connection.steamId
        if (activeSteamId != null) {
            return activeSteamId.accountID.toInt()
        }
        return PrefManager.steamUserAccountId.takeIf { it > 0 }
    }

    fun getPersonaName(): String = PrefManager.steamUserName

    fun getAvatarHash(): String = PrefManager.steamUserAvatarHash

    fun substituteSteamIdTokens(value: String): String {
        val steamId64 = getSteamId64()?.toString() ?: "0"
        val steam3AccountId = getSteam3AccountId()?.toString() ?: "0"
        return value
            .replace("{64BitSteamID}", steamId64)
            .replace("{Steam3AccountID}", steam3AccountId)
    }

    fun cachePersona(name: String?, avatarHash: String?) {
        if (!name.isNullOrBlank()) {
            PrefManager.steamUserName = name
        }
        if (!avatarHash.isNullOrBlank()) {
            PrefManager.steamUserAvatarHash = avatarHash
        }
    }
}
