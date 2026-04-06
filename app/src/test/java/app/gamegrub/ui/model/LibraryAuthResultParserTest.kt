package app.gamegrub.ui.model

import android.app.Activity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LibraryAuthResultParserTest {

    @Test
    fun resolveErrorMessage_nonOkResult_returnsProvidedError() {
        val payload = OAuthResultPayload(
            resultCode = Activity.RESULT_CANCELED,
            authCode = null,
            errorMessage = "Login failed",
        )

        val actual = LibraryAuthResultParser.resolveErrorMessage(
            payload = payload,
            cancelFallbackMessage = "Canceled",
        )

        assertEquals("Login failed", actual)
    }

    @Test
    fun resolveErrorMessage_nonOkResult_withoutError_returnsFallback() {
        val payload = OAuthResultPayload(
            resultCode = Activity.RESULT_CANCELED,
            authCode = null,
            errorMessage = null,
        )

        val actual = LibraryAuthResultParser.resolveErrorMessage(
            payload = payload,
            cancelFallbackMessage = "Canceled",
        )

        assertEquals("Canceled", actual)
    }

    @Test
    fun resolveErrorMessage_okResult_withoutCode_returnsFallbackOrError() {
        val payloadWithError = OAuthResultPayload(
            resultCode = Activity.RESULT_OK,
            authCode = null,
            errorMessage = "Missing code",
        )
        val payloadWithoutError = OAuthResultPayload(
            resultCode = Activity.RESULT_OK,
            authCode = null,
            errorMessage = null,
        )

        val withError = LibraryAuthResultParser.resolveErrorMessage(
            payload = payloadWithError,
            cancelFallbackMessage = "Canceled",
        )
        val withoutError = LibraryAuthResultParser.resolveErrorMessage(
            payload = payloadWithoutError,
            cancelFallbackMessage = "Canceled",
        )

        assertEquals("Missing code", withError)
        assertEquals("Canceled", withoutError)
    }

    @Test
    fun resolveErrorMessage_okResult_withCode_returnsNull() {
        val payload = OAuthResultPayload(
            resultCode = Activity.RESULT_OK,
            authCode = "auth-code",
            errorMessage = null,
        )

        val actual = LibraryAuthResultParser.resolveErrorMessage(
            payload = payload,
            cancelFallbackMessage = "Canceled",
        )

        assertNull(actual)
    }
}
