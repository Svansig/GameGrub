package app.gamegrub.data

import app.gamegrub.data.GameSource

sealed class UnifiedCredentials {
    abstract val gameSource: GameSource
    abstract val isValid: Boolean

    data class Steam(override val isValid: Boolean) : UnifiedCredentials() {
        override val gameSource: GameSource = GameSource.STEAM
    }

    data class GOG(
        val accessToken: String,
        val refreshToken: String,
        val userId: String,
        val username: String,
    ) : UnifiedCredentials() {
        override val gameSource: GameSource = GameSource.GOG
        override val isValid: Boolean = accessToken.isNotEmpty()
    }

    data class Epic(
        val accessToken: String,
        val refreshToken: String,
        val accountId: String,
        val displayName: String,
        val expiresAt: Long = 0,
    ) : UnifiedCredentials() {
        override val gameSource: GameSource = GameSource.EPIC
        override val isValid: Boolean = accessToken.isNotEmpty() && (expiresAt == 0L || expiresAt > System.currentTimeMillis())
    }

    data class Amazon(
        val accessToken: String,
        val refreshToken: String,
        val userId: String,
    ) : UnifiedCredentials() {
        override val gameSource: GameSource = GameSource.AMAZON
        override val isValid: Boolean = accessToken.isNotEmpty()
    }

    companion object {
        fun fromGOG(gog: app.gamegrub.data.GOGCredentials): UnifiedCredentials =
            GOG(gog.accessToken, gog.refreshToken, gog.userId, gog.username)

        fun fromEpic(epic: app.gamegrub.data.EpicCredentials): UnifiedCredentials =
            Epic(epic.accessToken, epic.refreshToken, epic.accountId, epic.displayName, epic.expiresAt)
    }
}
