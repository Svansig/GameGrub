package app.gamegrub.gateway.impl

import app.gamegrub.PrefManager
import app.gamegrub.gateway.PreferencesGateway
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

@Singleton
class PreferencesGatewayImpl @Inject constructor(
    private val prefManager: PrefManager,
) : PreferencesGateway {

    override fun getString(key: String, default: String): String {
        return prefManager.getString(key, default)
    }

    override fun getInt(key: String, default: Int): Int {
        return prefManager.getInt(key, default)
    }

    override fun getLong(key: String, default: Long): Long {
        return prefManager.getLong(key, default)
    }

    override fun getBoolean(key: String, default: Boolean): Boolean {
        return prefManager.getBoolean(key, default)
    }

    override fun getFloat(key: String, default: Float): Float {
        return prefManager.getFloat(key, default)
    }

    override suspend fun putString(key: String, value: String) {
        prefManager.putString(key, value)
    }

    override suspend fun putInt(key: String, value: Int) {
        prefManager.putInt(key, value)
    }

    override suspend fun putLong(key: String, value: Long) {
        prefManager.putLong(key, value)
    }

    override suspend fun putBoolean(key: String, value: Boolean) {
        prefManager.putBoolean(key, value)
    }

    override suspend fun putFloat(key: String, value: Float) {
        prefManager.putFloat(key, value)
    }

    override suspend fun remove(key: String) {
        prefManager.remove(key)
    }

    override fun observeString(key: String): Flow<String> {
        return MutableStateFlow(getString(key))
    }

    override fun observeInt(key: String): Flow<Int> {
        return MutableStateFlow(getInt(key))
    }

    override fun observeBoolean(key: String): Flow<Boolean> {
        return MutableStateFlow(getBoolean(key))
    }
}
