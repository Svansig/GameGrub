package app.gamegrub.service.steam.managers

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import `in`.dragonbra.javasteam.util.HardwareUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SteamDeviceIdentityManager @Inject constructor() {

    fun getMachineName(context: Context): String {
        return try {
            Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
                ?: Settings.System.getString(context.contentResolver, "device_name")
                ?: HardwareUtils.getMachineName()
        } catch (_: Exception) {
            HardwareUtils.getMachineName()
        }
    }

    @SuppressLint("HardwareIds")
    fun getUniqueDeviceId(context: Context): Int {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        return androidId.hashCode()
    }
}
