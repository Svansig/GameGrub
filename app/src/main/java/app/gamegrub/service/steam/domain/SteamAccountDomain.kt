package app.gamegrub.service.steam.domain

import app.gamegrub.service.steam.managers.SteamAuthService
import app.gamegrub.service.steam.managers.SteamDeviceIdentityManager
import app.gamegrub.service.steam.managers.SteamFriendsManager
import app.gamegrub.service.steam.managers.SteamUserManager
import javax.inject.Inject
import javax.inject.Singleton

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
)