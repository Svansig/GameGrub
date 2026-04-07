package app.gamegrub.service.steam.managers

import app.gamegrub.data.SteamControllerConfigDetail
import org.json.JSONObject
import timber.log.Timber
import java.io.File

/**
 * Downloads Steam Workshop controller templates used by template index routing.
 */
object SteamControllerWorkshopDownloadManager {
    fun tryDownloadWorkshopControllerConfig(
        templateIndex: Int,
        configDetails: List<SteamControllerConfigDetail>,
        appDirPath: String,
        configFileName: String,
        requiresWorkshopDownload: (Int) -> Boolean,
        selectConfig: (List<SteamControllerConfigDetail>) -> SteamControllerConfigDetail?,
        fetchPublishedFileDetailsJson: (Long) -> String?,
        downloadFileBytes: (String) -> ByteArray?,
    ): Boolean {
        if (!requiresWorkshopDownload(templateIndex)) {
            return false
        }

        val selectedConfig = selectConfig(configDetails) ?: return false
        val publishedFileId = selectedConfig.publishedFileId

        return runCatching {
            val detailsJson = fetchPublishedFileDetailsJson(publishedFileId)
            if (detailsJson.isNullOrEmpty()) {
                Timber.w("Empty response body for steam controller config %d", publishedFileId)
                return false
            }

            val fileUrl = extractFileUrl(detailsJson, publishedFileId) ?: return false
            val fileBytes = downloadFileBytes(fileUrl) ?: return false

            val configFile = File(appDirPath, configFileName)
            configFile.outputStream().use { output ->
                output.write(fileBytes)
            }
            Timber.i("Downloaded steam controller config %d to %s", publishedFileId, configFile.path)
            true
        }.getOrElse { error ->
            Timber.w(error, "Steam controller config download failed for %d", publishedFileId)
            false
        }
    }

    private fun extractFileUrl(responseBody: String, publishedFileId: Long): String? {
        val responseJson = JSONObject(responseBody)
        val responseData = responseJson.optJSONObject("response")
        if (responseData == null) {
            Timber.w(
                "Steam controller config $publishedFileId missing response data",
            )
            return null
        }

        val result = responseData.optInt("result", 0)
        val resultCount = responseData.optInt("resultcount", 0)
        if (result != 1 || resultCount < 1) {
            Timber.w(
                "Steam controller config $publishedFileId returned result=$result resultcount=$resultCount",
            )
            return null
        }

        val fileDetails = responseData
            .optJSONArray("publishedfiledetails")
            ?.optJSONObject(0)
        if (fileDetails == null) {
            Timber.w(
                "Steam controller config $publishedFileId missing publishedfiledetails",
            )
            return null
        }

        val fileUrl = fileDetails.optString("file_url", "").trim()
        if (fileUrl.isEmpty()) {
            Timber.w(
                "Steam controller config $publishedFileId missing fileUrl",
            )
            return null
        }

        return fileUrl
    }
}
