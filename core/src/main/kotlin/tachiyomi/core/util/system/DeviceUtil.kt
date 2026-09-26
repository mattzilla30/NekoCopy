package tachiyomi.core.util.system

import android.annotation.SuppressLint
import android.os.Build
import org.nekomanga.logging.TimberKt

object DeviceUtil {

    val isMiui by lazy { getSystemProperty("ro.miui.ui.version.name")?.isNotEmpty() ?: false }

    val isSamsung by lazy { Build.MANUFACTURER.equals("samsung", ignoreCase = true) }

    val invalidDefaultBrowsers =
        listOf("android", "com.huawei.android.internal.app", "com.zui.resolver")

    @SuppressLint("PrivateApi")
    private fun getSystemProperty(key: String?): String? {
        return try {
            Class.forName("android.os.SystemProperties")
                .getDeclaredMethod("get", String::class.java)
                .invoke(null, key) as String
        } catch (e: Exception) {
            TimberKt.w(e) { "Unable to use SystemProperties.get()" }
            null
        }
    }
}
