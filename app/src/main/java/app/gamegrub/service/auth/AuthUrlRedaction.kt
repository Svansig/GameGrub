package app.gamegrub.service.auth

import androidx.core.net.toUri

fun redactUrlForLogging(url: String?): String =
    url?.let {
        runCatching {
            it.toUri().buildUpon().clearQuery().fragment(null).build().toString()
        }.getOrDefault("<invalid-url>")
    } ?: "null"
