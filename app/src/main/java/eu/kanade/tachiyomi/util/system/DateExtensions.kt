package eu.kanade.tachiyomi.util.system

import android.text.format.DateUtils
import java.text.DateFormat
import java.util.Date

fun Date.toTimestampString(dateFormatter: DateFormat): String {
    val date = dateFormatter.format(this)
    val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(this)
    return "$date $time"
}

val Long.timeSpanFromNow: String
    get() = DateUtils.getRelativeTimeSpanString(this).toString()
