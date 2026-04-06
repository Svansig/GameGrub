package app.gamegrub.utils.general

import android.text.Html
import java.text.Normalizer

private val REGEX_UNACCENT = "\\p{M}+".toRegex()

/**
 * Extension functions relating to [String] as the receiver type.
 */

fun String.fromHtml(): String = Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY).toString()

fun CharSequence.unaccent(): String {
    val temp = Normalizer.normalize(this, Normalizer.Form.NFKD)
    return REGEX_UNACCENT.replace(temp, "")
}
