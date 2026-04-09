package app.gamegrub.service.steam.domain

import app.gamegrub.data.SteamFriend
import app.gamegrub.enums.LoginResult
import app.gamegrub.service.steam.managers.SteamAuthService
import app.gamegrub.service.steam.managers.SteamDeviceIdentityManager
import app.gamegrub.service.steam.managers.SteamFriendsManager
import app.gamegrub.service.steam.managers.SteamUserManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Account domain: authentication, user identity, persona, and device identity concerns.
 *
 * Coordinates:
 * - SteamAuthService: QR and credential-based authentication
 * - SteamUserManager: User identity and persona caching
 * - SteamFriendsManager: Friends list and persona state
 * - SteamDeviceIdentityManager: Device identity for Steam
 */
@Singleton
class SteamAccountDomain @Inject constructor(
    val authService: SteamAuthService,
    val userManager: SteamUserManager,
    val friendsManager: SteamFriendsManager,
    val deviceIdentityManager: SteamDeviceIdentityManager,
) {
    private val _loginResult = MutableStateFlow(LoginResult.Failed)
    val loginResult: StateFlow<LoginResult> = _loginResult.asStateFlow()

    private val _familyGroupMembers = MutableStateFlow<List<Int>>(emptyList())
    val familyGroupMembers: StateFlow<List<Int>> = _familyGroupMembers.asStateFlow()

    private val _localPersona = MutableStateFlow(SteamFriend())
    val localPersona: StateFlow<SteamFriend> = _localPersona.asStateFlow()

    private var _isLoggingOut = false
    val isLoggingOut: Boolean get() = _isLoggingOut

    fun setLoginResult(result: LoginResult) {
        _loginResult.value = result
    }

    fun setFamilyGroupMembers(members: List<Int>) {
        _familyGroupMembers.value = members
    }

    fun addFamilyGroupMember(accountId: Int) {
        _familyGroupMembers.value += accountId
    }

    fun updateLocalPersona(update: (SteamFriend) -> SteamFriend) {
        _localPersona.value = update(_localPersona.value)
    }

    fun setLoggingOut(loggingOut: Boolean) {
        _isLoggingOut = loggingOut
    }

    fun clearState() {
        _loginResult.value = LoginResult.Failed
        _familyGroupMembers.value = emptyList()
        _isLoggingOut = false
    }
}
