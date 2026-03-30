package app.gamegrub.service.steam.managers

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Session domain manager: launch/close app flow, session files, and ticket lifecycle.
 */
@Singleton
class SteamSessionManager @Inject constructor(
    val appSessionManager: SteamAppSessionManager,
    val sessionFilesManager: SteamSessionFilesManager,
    val ticketManager: SteamTicketManager,
)

