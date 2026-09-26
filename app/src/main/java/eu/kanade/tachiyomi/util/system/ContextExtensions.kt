package eu.kanade.tachiyomi.util.system

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.view.View
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.content.res.use
import androidx.core.net.toUri
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.ui.webview.WebViewActivity
import eu.kanade.tachiyomi.util.lang.orUnknownError
import org.nekomanga.R
import org.nekomanga.constants.MdConstants
import org.nekomanga.presentation.components.UiText

/**
 * Display a toast in this context.
 *
 * @param resource the text resource.
 * @param duration the duration of the toast. Defaults to short.
 */
fun Context.toast(@StringRes resource: Int, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, resource, duration).show()
}

/**
 * Display a toast in this context.
 *
 * @param text the text to display.
 * @param duration the duration of the toast. Defaults to short.
 */
fun Context.toast(text: String?, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, text.orEmpty(), duration).show()
}

/**
 * Display a toast in this context.
 *
 * @param text the text to display.
 * @param duration the duration of the toast. Defaults to short.
 */
fun Context.toast(text: UiText?, duration: Int = Toast.LENGTH_SHORT) {
    val message =
        when (text) {
            null -> ""
            is UiText.PluralsResource ->
                this.resources.getQuantityString(text.resourceId, text.count, *text.args)
            is UiText.String -> text.str
            is UiText.StringResource -> {
                if (text.args.isEmpty()) {
                    this.resources.getString(text.resourceId)
                } else {
                    this.resources.getString(text.resourceId, *text.args)
                }
            }
        }

    Toast.makeText(this, message, duration).show()
}

/**
 * Helper method to create a notification.
 *
 * @param id the channel id.
 * @param func the function that will execute inside the builder.
 * @return a notification to be displayed or updated.
 */
inline fun Context.notification(
    channelId: String,
    func: NotificationCompat.Builder.() -> Unit,
): Notification {
    val builder = NotificationCompat.Builder(this, channelId)
    builder.func()
    return builder.build()
}

/**
 * Returns the color for the given attribute.
 *
 * @param resource the attribute.
 */
@ColorInt
fun Context.getResourceColor(@AttrRes resource: Int): Int {
    return obtainStyledAttributes(intArrayOf(resource)).use { it.getColor(0, 0) }
}

/**
 * Returns the color from ContextCompat
 *
 * @param resource the color.
 */
fun Context.contextCompatColor(@ColorRes resource: Int): Int {
    return ContextCompat.getColor(this, resource)
}

/** Converts to dp. */
val Int.pxToDp: Int
    get() = (this / Resources.getSystem().displayMetrics.density).toInt()

val Float.pxToDp: Float
    get() = (this / Resources.getSystem().displayMetrics.density)

/** Converts to px. */
val Int.dpToPx: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

val Float.dpToPx: Float
    get() = (this * Resources.getSystem().displayMetrics.density)

/** Converts to px and takes into account LTR/RTL layout */
val Float.dpToPxEnd: Float
    get() =
        (this *
            Resources.getSystem().displayMetrics.density *
            if (Resources.getSystem().isLTR) 1 else -1)

/** Converts to px and takes into account LTR/RTL layout */
fun Float.dpToPxEnd(resources: Resources): Float {
    return this * resources.displayMetrics.density * if (resources.isLTR) 1 else -1
}

val Resources.isLTR
    get() = configuration.layoutDirection == View.LAYOUT_DIRECTION_LTR

fun Context.isTablet() = resources.configuration.smallestScreenWidthDp >= 600

/**
 * Helper method to create a notification builder.
 *
 * @param id the channel id.
 * @param block the function that will execute inside the builder.
 * @return a notification to be displayed or updated.
 */
fun Context.notificationBuilder(
    channelId: String,
    block: (NotificationCompat.Builder.() -> Unit)? = null,
): NotificationCompat.Builder {
    val builder =
        NotificationCompat.Builder(this, channelId)
            .setColor(ContextCompat.getColor(this, R.color.ic_launcher_background))
    if (block != null) {
        builder.block()
    }
    return builder
}

fun Context.isLandscape(): Boolean {
    return resources.configuration?.orientation == Configuration.ORIENTATION_LANDSCAPE
}

/** Property to get the notification manager from the context. */
val Context.notificationManager: NotificationManager
    get() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

/** Property to get the connectivity manager from the context. */
val Context.connectivityManager: ConnectivityManager
    get() = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

/** Property to get the power manager from the context. */
fun Context.defaultBrowserPackageName(): String? {
    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://"))
    return packageManager
        .resolveActivity(browserIntent, PackageManager.MATCH_DEFAULT_ONLY)
        ?.activityInfo
        ?.packageName
        ?.takeUnless { it in DeviceUtil.invalidDefaultBrowsers }
}

fun Context.openInWebView(url: String, title: String = "") {
    val intent = WebViewActivity.newIntent(this.applicationContext, url, title)
    startActivity(intent)
}

fun Context.openInBrowser(url: String, forceDefaultBrowser: Boolean = false) {
    if (url.contains(MdConstants.baseUrl)) {
        this.openInBrowser(url.toUri(), true)
    } else {
        this.openInBrowser(url.toUri(), forceDefaultBrowser)
    }
}

fun Context.openInBrowser(uri: Uri, forceDefaultBrowser: Boolean = false) {
    try {
        val intent =
            Intent(Intent.ACTION_VIEW, uri).apply {
                // Force default browser so that verified extensions don't re-open Neko
                if (forceDefaultBrowser) {
                    defaultBrowserPackageName()?.let { setPackage(it) }
                }
            }
        startActivity(intent)
    } catch (e: Exception) {
        toast(e.message.orUnknownError(this))
    }
}

fun Context.isInNightMode(): Boolean {
    val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return currentNightMode == Configuration.UI_MODE_NIGHT_YES
}

fun Context.getActivity(): AppCompatActivity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is AppCompatActivity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}

fun Context.appDelegateNightMode(): Int {
    return if (isInNightMode()) {
        AppCompatDelegate.MODE_NIGHT_YES
    } else {
        AppCompatDelegate.MODE_NIGHT_NO
    }
}

fun Context.isOnline(): Boolean {
    val networkCapabilities = connectivityManager.activeNetwork ?: return false
    val actNw = connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
    return (NetworkCapabilities.TRANSPORT_CELLULAR..NetworkCapabilities.TRANSPORT_LOWPAN).any(
        actNw::hasTransport
    )
}

fun Context.sharedCacheDir(): UniFile? {
    val uniFile = UniFile.fromFile(this.cacheDir)?.createDirectory("shared_image")
    uniFile?.listFiles()?.forEach { if (it.isFile) it.delete() }
    return uniFile
}
