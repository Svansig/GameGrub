package app.gamegrub.ui.model

import android.app.Activity

internal data class OAuthResultPayload(
    val resultCode: Int,
    val authCode: String?,
    val errorMessage: String?,
)

internal object LibraryAuthResultParser {
    fun resolveErrorMessage(
        payload: OAuthResultPayload,
        cancelFallbackMessage: String,
    ): String? {
        if (payload.resultCode != Activity.RESULT_OK) {
            return payload.errorMessage ?: cancelFallbackMessage
        }

        return if (payload.authCode == null) {
            payload.errorMessage ?: cancelFallbackMessage
        } else {
            null
        }
    }
}

