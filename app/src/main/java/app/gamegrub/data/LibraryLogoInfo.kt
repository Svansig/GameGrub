package app.gamegrub.data

import app.gamegrub.enums.Language
import kotlinx.serialization.Serializable

@Serializable
data class LibraryLogoInfo(
    val image: Map<Language, String> = emptyMap(),
    val image2x: Map<Language, String> = emptyMap(),
)
