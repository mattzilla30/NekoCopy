package eu.kanade.tachiyomi.util.lang

import android.content.Context
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.annotation.StringRes
import java.util.Locale
import org.nekomanga.core.R
import org.nekomanga.domain.network.ResultError
import tachiyomi.core.util.system.getResourceColor

fun String.capitalizeWords(): String {
    val firstReplace =
        split(" ").joinToString(" ") {
            it.replaceFirstChar { text -> text.titlecase(Locale.getDefault()) }
        }
    return firstReplace.split("-").joinToString("-") {
        it.replaceFirstChar { text -> text.titlecase(Locale.getDefault()) }
    }
}

fun String.capitalized(): String {
    return this.replaceFirstChar {
        if (it.isLowerCase()) {
            it.titlecase(Locale.US)
        } else {
            it.toString()
        }
    }
}

fun String.withSubtitle(context: Context, @StringRes subtitleRes: Int) =
    withSubtitle(context, context.getString(subtitleRes))

fun String.withSubtitle(context: Context, subtitle: String): Spanned {
    val spannable = SpannableStringBuilder(this + "\n" + subtitle)
    spannable.setSpan(
        ForegroundColorSpan(context.getResourceColor(android.R.attr.textColorSecondary)),
        this.length + 1,
        spannable.length,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
    )
    return spannable
}

private val uuidFormatLines = arrayOf(8, 13, 18, 23)
private val uuidFormatDigits = arrayOf((0..7), (9..12), (14..17), (19..22), (24..35))

/** Check if a string is in UUID format. */
fun String.isUUID() =
    this.length == 36 &&
        uuidFormatLines.all { idx -> this[idx] == '-' } &&
        uuidFormatDigits.all { range ->
            range.all { idx ->
                this[idx].let { char -> char in '0'..'9' || char in 'a'..'f' || char in 'A'..'F' }
            }
        }

fun String.toResultError() = ResultError.Generic(errorString = this)

fun String?.orUnknownError(context: Context): String {
    if (!isNullOrBlank()) return this
    return context.getString(R.string.unknown_error)
}
