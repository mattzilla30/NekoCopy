package org.nekomanga.presentation.screens.stats

import android.text.SpannableStringBuilder
import java.util.concurrent.TimeUnit

object StatsHelper {

    fun Long.getReadDuration(blankValue: String = "0"): String {
        val days = TimeUnit.MILLISECONDS.toDays(this)
        val hours = TimeUnit.MILLISECONDS.toHours(this) % 24
        val minutes = TimeUnit.MILLISECONDS.toMinutes(this) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(this) % 60

        return SpannableStringBuilder()
            .apply {
                if (days != 0L) append("${days}d")
                if (hours != 0L) append("${hours}h")
                if (minutes != 0L && days == 0L) append("${minutes}min")
                if (seconds != 0L && days == 0L && hours == 0L) append("${seconds}s")
            }
            .replace(Regex("(\\D)(?=\\d)"), "$0 ")
            .ifBlank { blankValue }
    }
}
