package app.gamegrub.service.steam

import app.gamegrub.Constants

fun String.getAvatarURL(): String =
    this.ifEmpty { null }
        ?.takeIf { avatarHash -> avatarHash.isNotEmpty() && !avatarHash.all { it == '0' } }
        ?.let { "${Constants.Persona.AVATAR_BASE_URL}${it.substring(0, 2)}/${it}_full.jpg" }
        ?: Constants.Persona.MISSING_AVATAR_URL

fun Long.getProfileUrl(): String = "${Constants.Persona.PROFILE_URL}$this/"

