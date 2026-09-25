package org.nekomanga.logging

import android.util.Log
import java.util.concurrent.CancellationException as JvmCancellationException
import kotlin.coroutines.cancellation.CancellationException
import timber.log.Timber

/**
 * Release builds log INFO and above to logcat, so CrashLogUtil can include them in a dumped crash
 * log. Cancellations are expected control flow and never get logged.
 */
class ReleaseLogTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (t is CancellationException || t is JvmCancellationException) return
        if (priority < Log.INFO || Timber.forest().any { it is Timber.DebugTree }) return

        val logMessage = if (t != null) "$message\n${Log.getStackTraceString(t)}" else message
        Log.println(priority, tag ?: "Kitty", logMessage)
    }
}
