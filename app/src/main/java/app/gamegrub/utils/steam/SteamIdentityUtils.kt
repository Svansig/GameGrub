package app.gamegrub.utils.steam

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import app.gamegrub.service.steam.SteamService
import `in`.dragonbra.javasteam.util.HardwareUtils

object SteamIdentityUtils {

    fun removeSpecialChars(value: String): String {
        val service = SteamService.instance
        if (service != null) {
            return service.userManager.removeSpecialChars(value)
        }
        return value.replace(Regex("[^\\u0000-\\u007F]"), "")
    }

    fun getMachineName(context: Context): String {
        val service = SteamService.instance
        if (service != null) {
            return service.deviceIdentityManager.getMachineName(context)
        }
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
        val service = SteamService.instance
        if (service != null) {
            return service.deviceIdentityManager.getUniqueDeviceId(context)
        }
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        return androidId.hashCode()
    }

    fun getSteamId64(): Long? {
        val service = SteamService.instance
        if (service != null) {
            return service.userManager.getSteamId64()
        }
        return SteamService.userSteamId?.convertToUInt64()
    }

    fun getSteam3AccountId(): Long? {
        val service = SteamService.instance
        if (service != null) {
            return service.userManager.getSteam3AccountId()
        }
        return SteamService.userSteamId?.accountID
    }
}
