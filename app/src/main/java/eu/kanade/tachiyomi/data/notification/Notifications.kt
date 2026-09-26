package eu.kanade.tachiyomi.data.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import eu.kanade.tachiyomi.util.system.notificationManager
import org.nekomanga.R

/** Class to manage the basic information of all the notifications used in the app. */
object Notifications {

    /** Common notification channel and ids used anywhere. */
    const val CHANNEL_COMMON = "common_channel"
    const val ID_DOWNLOAD_IMAGE = 2

    /** Notification channel used for crash log file sharing. */
    const val CHANNEL_CRASH_LOGS = "crash_logs_channel"
    const val ID_CRASH_LOGS = -601

    /** Notification channel used for Incognito Mode */
    const val CHANNEL_INCOGNITO_MODE = "incognito_mode_channel"
    const val ID_INCOGNITO_MODE = -701

    /** Channels and groups of removed features, deleted from existing installs. */
    private val deprecatedChannels =
        listOf(
            "library_channel",
            "installing_channel",
            "updated_channel",
            "downloader_error_channel",
            "downloader_progress_channel",
            "library_progress_channel",
            "library_errors_channel",
            "library_skipped_channel",
            "authentication_channel",
            "status_channel",
            "tracking_channel",
            "new_chapters_channel",
            "backup_restore_progress_channel",
            "backup_restore_complete_channel",
        )
    private val deprecatedGroups =
        listOf(
            "org.nekomanga.APP_UPDATES",
            "group_downloader",
            "group_library",
            "group_backup_restore",
        )

    /**
     * Creates the notification channels introduced in Android Oreo.
     *
     * @param context The application context.
     */
    fun createChannels(context: Context) {
        val channels =
            listOf(
                NotificationChannel(
                    CHANNEL_COMMON,
                    context.getString(R.string.common),
                    NotificationManager.IMPORTANCE_LOW,
                ),
                NotificationChannel(
                    CHANNEL_CRASH_LOGS,
                    context.getString(R.string.channel_crash_logs),
                    NotificationManager.IMPORTANCE_HIGH,
                ),
                NotificationChannel(
                        CHANNEL_INCOGNITO_MODE,
                        context.getString(R.string.incognito_mode),
                        NotificationManager.IMPORTANCE_LOW,
                    )
                    .apply { lockscreenVisibility = Notification.VISIBILITY_SECRET },
            )
        context.notificationManager.createNotificationChannels(channels)

        deprecatedChannels.forEach(context.notificationManager::deleteNotificationChannel)
        deprecatedGroups.forEach(context.notificationManager::deleteNotificationChannelGroup)
    }
}
