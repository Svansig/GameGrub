package app.gamegrub.utils.steam

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import app.gamegrub.service.steam.SteamService
import `in`.dragonbra.javasteam.util.HardwareUtils

object SteamIdentityUtils {

    fun removeSpecialChars(value: String): String = value.replace(Regex("[^\\u0000-\\u007F]"), "")

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

    fun getSteamId64(): Long? = SteamService.userSteamId?.convertToUInt64()

    fun getSteam3AccountId(): Long? = SteamService.userSteamId?.accountID
}
