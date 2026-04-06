package app.gamegrub.service.steam.domain

import `in`.dragonbra.javasteam.steam.handlers.steamapps.License
import org.json.JSONObject
import timber.log.Timber

object LicenseSerializer {

    fun serializeLicense(license: License): String {
        return try {
            val jsonObj = JSONObject().apply {
                put("packageID", license.packageID)
                put("lastChangeNumber", license.lastChangeNumber)
                put("timeCreated", license.timeCreated.time)
                put("timeNextProcess", license.timeNextProcess.time)
                put("minuteLimit", license.minuteLimit)
                put("minutesUsed", license.minutesUsed)
                put("paymentMethod", license.paymentMethod.code())
                put("licenseFlags", org.json.JSONArray(license.licenseFlags.map { it.code() }))
                put("purchaseCode", license.purchaseCode)
                put("licenseType", license.licenseType.code())
                put("territoryCode", license.territoryCode)
                put("accessToken", license.accessToken)
                put("ownerAccountID", license.ownerAccountID)
                put("masterPackageID", license.masterPackageID)
            }
            jsonObj.toString()
        } catch (e: Exception) {
            Timber.e(e, "Failed to serialize license: ${e.message}")
            ""
        }
    }
}
