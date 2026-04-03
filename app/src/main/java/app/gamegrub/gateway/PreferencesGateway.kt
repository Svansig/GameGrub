package app.gamegrub.gateway

import kotlinx.coroutines.flow.Flow

interface PreferencesGateway {
    fun getString(key: String, default: String = ""): String

    fun getInt(key: String, default: Int = 0): Int

    fun getLong(key: String, default: Long = 0L): Long

    fun getBoolean(key: String, default: Boolean = false): Boolean

    fun getFloat(key: String, default: Float = 0f): Float

    suspend fun putString(key: String, value: String)

    suspend fun putInt(key: String, value: Int)

    suspend fun putLong(key: String, value: Long)

    suspend fun putBoolean(key: String, value: Boolean)

    suspend fun putFloat(key: String, value: Float)

    suspend fun remove(key: String)

    fun observeString(key: String): Flow<String>

    fun observeInt(key: String): Flow<Int>

    fun observeBoolean(key: String): Flow<Boolean>
}
