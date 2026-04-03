package app.gamegrub.config

import app.gamegrub.data.GameSource

data class StoreConfig(
    val source: GameSource,
    val enabled: Boolean = true,
    val autoSync: Boolean = true,
    val syncIntervalMinutes: Int = 15,
    val downloadParallelism: Int = 2,
    val cloudSyncEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
)

object StoreConfigProvider {
    private val defaultConfigs = GameSource.entries.map { source ->
        source to StoreConfig(source = source)
    }.toMap()

    fun getConfig(source: GameSource): StoreConfig {
        return defaultConfigs[source] ?: StoreConfig(source = source)
    }

    fun getAllConfigs(): List<StoreConfig> {
        return defaultConfigs.values.toList()
    }

    fun updateConfig(source: GameSource, config: StoreConfig) {
        defaultConfigs[source]?.let {
            defaultConfigs[source] = config
        }
    }
}
