package eu.kanade.tachiyomi.data.notification

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.util.storage.getUriWithAuthority
import eu.kanade.tachiyomi.util.system.getParcelableExtraCompat
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.notificationManager
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.system.withUIContext
import java.io.File
import org.nekomanga.BuildConfig.APPLICATION_ID as ID
import org.nekomanga.R
import org.nekomanga.logging.TimberKt
import tachiyomi.core.util.storage.DiskUtil
import uy.kohesive.injekt.api.get

/**
 * Global [BroadcastReceiver] that runs on UI thread Pending Broadcasts should be made from here.
 * NOTE: Use local broadcasts if possible.
 */
class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            // Dismiss notification
            ACTION_DISMISS_NOTIFICATION ->
                dismissNotification(context, intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1))
            // Delete image and dismiss notification
            ACTION_DELETE_IMAGE -> {
                val pendingResult = goAsync()
                launchIO {
                    try {
                        val uri =
                            intent.getParcelableExtraCompat<Uri>(EXTRA_URI)
                                ?: intent.getStringExtra(EXTRA_FILE_LOCATION)?.let { path ->
                                    if (path.startsWith("content://")) Uri.parse(path)
                                    else File(path).toUri()
                                }
                        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
                        if (uri != null) {
                            deleteImage(context, uri, notificationId)
                        } else {
                            TimberKt.e { "ACTION_DELETE_IMAGE received with null URI extra" }
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            // Share crash dump file
            ACTION_SHARE_CRASH_LOG ->
                shareFile(
                    context,
                    intent.getParcelableExtraCompat<Uri>(EXTRA_URI)!!,
                    "text/plain",
                    intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1),
                )
        }
    }

    /**
     * Dismiss the notification
     *
     * @param notificationId the id of the notification
     */
    private fun dismissNotification(context: Context, notificationId: Int) {
        context.notificationManager.cancel(notificationId)
    }

    /**
     * Called to start share intent to share backup file
     *
     * @param context context of application
     * @param path path of file
     * @param notificationId id of notification
     */
    private fun shareFile(context: Context, uri: Uri, fileMimeType: String, notificationId: Int) {
        val sendIntent =
            Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newRawUri(null, uri)
                type = fileMimeType
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
        // Dismiss notification
        dismissNotification(context, notificationId)
        // Launch share activity
        context.startActivity(sendIntent)
    }

    /**
     * Called to delete image
     *
     * @param uri uri of the saved image
     * @param notificationId id of notification
     */
    private suspend fun deleteImage(context: Context, uri: Uri, notificationId: Int) {
        val deleted = UniFile.fromUri(context, uri)?.delete() == true
        if (deleted) {
            // Dismiss notification only after successful deletion
            dismissNotification(context, notificationId)
            DiskUtil.scanMedia(context, uri)
        } else {
            TimberKt.e { "Could not delete saved page $uri" }
            withUIContext { context.toast(R.string.could_not_delete_picture) }
        }
    }

    companion object {
        private const val NAME = "NotificationReceiver"

        // Called to delete image.
        private const val ACTION_DELETE_IMAGE = "$ID.$NAME.DELETE_IMAGE"

        private const val ACTION_SHARE_CRASH_LOG = "$ID.$NAME.SEND_CRASH_LOG"

        // Called to dismiss notification.
        private const val ACTION_DISMISS_NOTIFICATION = "$ID.$NAME.ACTION_DISMISS_NOTIFICATION"

        // Value containing uri.
        private const val EXTRA_URI = "$ID.$NAME.URI"

        // Value containing file location (legacy compatibility for in-flight notifications).
        private const val EXTRA_FILE_LOCATION = "$ID.$NAME.FILE_LOCATION"

        // Value containing notification id.
        private const val EXTRA_NOTIFICATION_ID = "$ID.$NAME.NOTIFICATION_ID"

        // Value containing group id.
        private const val EXTRA_GROUP_ID = "$ID.$NAME.EXTRA_GROUP_ID"

        // Value containing manga id.
        private const val EXTRA_MANGA_ID = "$ID.$NAME.EXTRA_MANGA_ID"

        // Value containing chapter id.
        private const val EXTRA_CHAPTER_ID = "$ID.$NAME.EXTRA_CHAPTER_ID"

        // Value containing chapter url.
        private const val EXTRA_CHAPTER_URL = "$ID.$NAME.EXTRA_CHAPTER_URL"

        /**
         * Returns [PendingIntent] that starts a service which dismissed the notification
         *
         * @param context context of application
         * @param notificationId id of notification
         * @return [PendingIntent]
         */
        internal fun dismissNotification(
            context: Context,
            notificationId: Int,
            groupId: Int? = null,
        ) {
            val groupKey =
                context.notificationManager.activeNotifications
                    .find { it.id == notificationId }
                    ?.groupKey
            if (groupId != null && groupId != 0 && groupKey != null && groupKey.isNotEmpty()) {
                val notifications =
                    context.notificationManager.activeNotifications.filter {
                        it.groupKey == groupKey
                    }
                if (notifications.size == 2) {
                    context.notificationManager.cancel(groupId)
                    return
                }
            }
            context.notificationManager.cancel(notificationId)
        }

        /**
         * Returns [PendingIntent] that starts a service which cancels the notification and starts a
         * share activity
         *
         * @param context context of application
         * @param uri uri of the saved image
         * @param notificationId id of notification
         * @return [PendingIntent]
         */
        internal fun shareImagePendingBroadcast(
            context: Context,
            uri: Uri,
            notificationId: Int,
        ): PendingIntent {
            val shareIntent =
                Intent(Intent.ACTION_SEND).apply {
                    val stream = uri.getUriWithAuthority(context)
                    putExtra(Intent.EXTRA_STREAM, stream)
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    clipData = ClipData.newRawUri(null, stream)
                    type = "image/*"
                }
            val chooser =
                Intent.createChooser(shareIntent, context.getString(R.string.share)).apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    clipData = shareIntent.clipData
                }
            return PendingIntent.getActivity(
                context,
                0,
                chooser,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        /**
         * Returns [PendingIntent] that starts a service which removes an image from disk
         *
         * @param context context of application
         * @param uri uri of the saved image
         * @param notificationId id of notification
         * @return [PendingIntent]
         */
        internal fun deleteImagePendingBroadcast(
            context: Context,
            uri: Uri,
            notificationId: Int,
        ): PendingIntent {
            val intent =
                Intent(context, NotificationReceiver::class.java).apply {
                    action = ACTION_DELETE_IMAGE
                    putExtra(EXTRA_URI, uri)
                    putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                }
            return PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        /**
         * Returns [PendingIntent] that opens the error log file in an external viewer
         *
         * @param context context of application
         * @param uri uri of error log file
         * @return [PendingIntent]
         */
        internal fun openErrorOrSkippedLogPendingActivity(
            context: Context,
            uri: Uri?,
        ): PendingIntent {
            val intent =
                Intent().apply {
                    action = Intent.ACTION_VIEW
                    setDataAndType(uri, "text/plain")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
            return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        }

        /**
         * Returns [PendingIntent] that starts a share activity for a crash log dump file.
         *
         * @param context context of application
         * @param uri uri of file
         * @param notificationId id of notification
         * @return [PendingIntent]
         */
        internal fun shareCrashLogPendingBroadcast(
            context: Context,
            uri: Uri,
            notificationId: Int,
        ): PendingIntent {
            val intent =
                Intent(context, NotificationReceiver::class.java).apply {
                    action = ACTION_SHARE_CRASH_LOG
                    putExtra(EXTRA_URI, uri)
                    putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                }
            return PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}
