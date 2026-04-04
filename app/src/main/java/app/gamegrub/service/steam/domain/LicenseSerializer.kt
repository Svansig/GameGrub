package app.gamegrub.service.steam.domain

import android.util.Base64
import `in`.dragonbra.javasteam.enums.ELicenseFlags
import `in`.dragonbra.javasteam.enums.ELicenseType
import `in`.dragonbra.javasteam.enums.EPaymentMethod
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientserver
import `in`.dragonbra.javasteam.steam.handlers.steamapps.License
import `in`.dragonbra.javasteam.types.DepotManifest
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.Date
import java.util.EnumSet
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
