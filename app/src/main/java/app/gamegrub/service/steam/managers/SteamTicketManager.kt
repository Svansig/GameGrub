package app.gamegrub.service.steam.managers

import app.gamegrub.service.steam.di.SteamTicketClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SteamTicketManager @Inject constructor(
    private val ticketClient: SteamTicketClient,
) {
    /**
     * Get encrypted app ticket, fetching from Steam if cache is stale.
     * Returns the ticket bytes, or null if unavailable.
     */
    suspend fun getEncryptedAppTicket(appId: Int): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val cached = ticketClient.getEncryptedAppTicket(appId)
            if (cached != null) {
                return@withContext cached
            }
            ticketClient.fetchAndCacheEncryptedAppTicket(appId)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get encrypted app ticket for $appId")
            null
        }
    }

    suspend fun getEncryptedAppTicketBase64(appId: Int): String? = withContext(Dispatchers.IO) {
        val ticket = getEncryptedAppTicket(appId)
        ticket?.let { android.util.Base64.encodeToString(it, android.util.Base64.DEFAULT) }
    }

    suspend fun storeEncryptedAppTicket(appId: Int, ticket: ByteArray) = withContext(Dispatchers.IO) {
        try {
            ticketClient.storeEncryptedAppTicket(appId, ticket)
        } catch (e: Exception) {
            Timber.e(e, "Failed to store encrypted app ticket for $appId")
        }
    }

    fun clearAllTickets() {
        ticketClient.clearAllTickets()
    }
}
