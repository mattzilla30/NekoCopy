package eu.kanade.tachiyomi

import android.app.job.JobScheduler
import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.preference.PreferenceKeys
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import org.nekomanga.BuildConfig

object Migrations {

    /**
     * Performs a migration when the application is updated.
     *
     * @param preferences Preferences of the application.
     * @return true if a migration is performed, false otherwise.
     */
    fun upgrade(preferences: PreferencesHelper): Boolean {
        val oldVersion = preferences.lastVersionCode().get()
        if (oldVersion >= BuildConfig.VERSION_CODE) return false
        preferences.lastVersionCode().set(BuildConfig.VERSION_CODE)
        clearRemovedFeatures(preferences.context)
        return true
    }

    /** Clears what removed features left behind on existing installs. */
    private fun clearRemovedFeatures(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit {
            remove("notify_on_install_complete")
            remove(PreferenceKeys.shouldAutoUpdate)
            // Merged sources were removed.
            remove("pref_filter_merged_key")
            remove("suwayomi_login_mode")
            // Sign-in and trackers were removed, so drop their credentials.
            prefs.all.keys
                .filter { key -> REMOVED_CREDENTIAL_PREFIXES.any { key.startsWith(it) } }
                .forEach(::remove)
        }
        // Library updates, backups, downloads and tracker syncs ran as scheduled jobs. Kitty
        // schedules no jobs now, so cancel every job left from an older version and drop the
        // job database the scheduler kept.
        context.getSystemService(JobScheduler::class.java)?.cancelAll()
        context.deleteDatabase("androidx.work.workdb")
        // Library covers were kept in their own folder.
        CoverCache(context).deleteLibraryCovers()
    }

    private val REMOVED_CREDENTIAL_PREFIXES =
        listOf(
            "mangadex_username",
            "track_token_",
            "track_token_expired_",
            "pref_mangasync_username_",
            "pref_mangasync_password_",
            "mangabaka_code_verifier",
        )
}
