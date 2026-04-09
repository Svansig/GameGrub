package app.gamegrub.api.steamMetadata

import app.gamegrub.network.NetworkManager
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import timber.log.Timber

object SteamMetadataFetcher {

    val http = NetworkManager.http.newBuilder()
        .readTimeout(5, TimeUnit.MINUTES)
        .callTimeout(0, TimeUnit.MILLISECONDS)
        .protocols(listOf(Protocol.HTTP_1_1))
        .build()

    fun fetchDirect3DMajor(steamAppId: Int, callback: (Int) -> Unit) {
        Timber.i("[DX Fetch] Starting fetchDirect3DMajor for appId=%d", steamAppId)
        val where = URLEncoder.encode("Infobox_game.Steam_AppID HOLDS \"$steamAppId\"", "UTF-8")
        val url =
            "https://pcgamingwiki.com/w/api.php" +
                "?action=cargoquery" +
                "&tables=Infobox_game,AP" +
                "I&join_on=Infobox_game._pageID=API._pageID" +
                "&fields=API.Direct3D_versions" +
                "&where=$where" +
                "&format=json"

        Timber.i("[DX Fetch] Starting fetchDirect3DMajor for query=%s", url)

        http.newCall(Request.Builder().url(url).build()).enqueue(
            object : Callback {
                override fun onFailure(call: Call, e: IOException) = callback(-1)

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        try {
                            val body = it.body.string()
                            if (body.isEmpty()) {
                                callback(-1)
                                return
                            }
                            Timber.i("[DX Fetch] Raw body fetchDirect3DMajor for body=%s", body)
                            val arr = JSONObject(body)
                                .optJSONArray("cargoquery") ?: run {
                                callback(-1)
                                return
                            }

                            val raw = arr.optJSONObject(0)
                                ?.optJSONObject("title")
                                ?.optString("Direct3D versions")
                                ?.trim() ?: ""

                            Timber.i("[DX Fetch] Raw fetchDirect3DMajor for raw=%s", raw)

                            val dx = Regex("\\b(9|10|11|12)\\b")
                                .findAll(raw).maxOfOrNull { match -> match.value.toInt() } ?: -1

                            Timber.i("[DX Fetch] dx fetchDirect3DMajor is dx=%d", dx)

                            callback(dx)
                        } catch (_: Exception) {
                            callback(-1)
                        }
                    }
                }
            },
        )
    }
}
