package app.gamegrub.gateway

import app.gamegrub.data.GameSource

/**
 * Gateway interface for authentication state across all game platforms.
 * Provides a unified API for querying login state without direct service dependencies.
 */
interface AuthStateGateway {
    /**
     * Checks if a specific platform has stored credentials.
     * @param source The game platform to check.
     * @return true if credentials exist, false otherwise.
     */
    fun hasStoredCredentials(source: GameSource): Boolean

    /**
     * Checks if user is actively logged in to a platform.
     * @param source The game platform to check.
     * @return true if actively logged in, false otherwise.
     */
    fun isLoggedIn(source: GameSource): Boolean

    /**
     * Gets the set of platforms with stored credentials.
     * @return Set of GameSource platforms with credentials.
     */
    fun getLoggedInStores(): Set<GameSource>
}
