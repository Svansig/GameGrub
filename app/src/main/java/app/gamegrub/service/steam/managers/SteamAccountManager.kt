package app.gamegrub.service.steam.managers

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Account domain manager: authentication, user identity, persona, and device identity concerns.
 */
@Singleton
class SteamAccountManager @Inject constructor(
    val authService: SteamAuthService,
    val userManager: SteamUserManager,
    val friendsManager: SteamFriendsManager,
    val deviceIdentityManager: SteamDeviceIdentityManager,
)

