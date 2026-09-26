package eu.kanade.tachiyomi.data.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.util.storage.getUriWithAuthority

/** Class that manages [PendingIntent] of activity's */
object NotificationHandler {

    /**
     * Returns [PendingIntent] that starts a gallery activity
     *
     * @param context context of application
     * @param file file containing image
     */
    internal fun openImagePendingActivity(context: Context, file: UniFile): PendingIntent {
        val intent =
            Intent(Intent.ACTION_VIEW).apply {
                val uri = file.uri.getUriWithAuthority(context)
                setDataAndType(uri, "image/*")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
