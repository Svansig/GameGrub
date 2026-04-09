package app.gamegrub.service.auth

import android.content.Context
import app.gamegrub.service.amazon.AmazonService
import app.gamegrub.service.epic.EpicService
import app.gamegrub.service.gog.GOGService
import app.gamegrub.service.steam.SteamService

object PlatformAuthUtils {
    fun isSignedInToAnyPlatform(context: Context): Boolean =
        SteamService.isLoggedIn ||
            GOGService.hasStoredCredentials(context) ||
            EpicService.hasStoredCredentials(context) ||
            AmazonService.hasStoredCredentials(context)
}
