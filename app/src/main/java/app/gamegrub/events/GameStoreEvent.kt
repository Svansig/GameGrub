package app.gamegrub.events

import app.gamegrub.data.GameSource

sealed class GameStoreEvent {
    data class LibraryUpdated(val source: GameSource) : GameStoreEvent()
    data class GameInstalled(val appId: String) : GameStoreEvent()
    data class GameUninstalled(val appId: String) : GameStoreEvent()
    data class GameDownloadStarted(val appId: String) : GameStoreEvent()
    data class GameDownloadCompleted(val appId: String) : GameStoreEvent()
    data class GameDownloadFailed(val appId: String, val error: String) : GameStoreEvent()
    data class CloudSyncStarted(val appId: String) : GameStoreEvent()
    data class CloudSyncCompleted(val appId: String) : GameStoreEvent()
    data class CloudSyncFailed(val appId: String, val error: String) : GameStoreEvent()
    data class AuthStateChanged(val source: GameSource, val isLoggedIn: Boolean) : GameStoreEvent()
    data class ServiceStateChanged(val source: GameSource, val isRunning: Boolean) : GameStoreEvent()
    data class Error(val source: GameSource?, val code: String, val message: String) : GameStoreEvent()
}

object GameStoreEventBus {
    private val listeners = mutableListOf<(GameStoreEvent) -> Unit>()

    fun subscribe(listener: (GameStoreEvent) -> Unit) {
        listeners.add(listener)
    }

    fun unsubscribe(listener: (GameStoreEvent) -> Unit) {
        listeners.remove(listener)
    }

    fun emit(event: GameStoreEvent) {
        listeners.forEach { it(event) }
    }
}

inline fun subscribeToGameEvents(crossinline handler: (GameStoreEvent) -> Unit) {
    GameStoreEventBus.subscribe { event -> handler(event) }
}
