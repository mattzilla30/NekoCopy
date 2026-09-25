package eu.kanade.tachiyomi

import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.work.WorkManager
import eu.kanade.tachiyomi.data.backup.BackupCreatorJob
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
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
        val context = preferences.context
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        // The in-app updater is gone. Clear what it left behind on existing installs.
        prefs.edit {
            remove("notify_on_install_complete")
            remove(PreferenceKeys.shouldAutoUpdate)
        }
        WorkManager.getInstance(context).cancelAllWorkByTag("UpdateChecker")
        val oldVersion = preferences.lastVersionCode().get()
        if (oldVersion < BuildConfig.VERSION_CODE) {
            preferences.lastVersionCode().set(BuildConfig.VERSION_CODE)

            // Always set up background tasks to ensure they're running
            LibraryUpdateJob.setupTask(context)
            BackupCreatorJob.setupTask(context, 12)

            return true
        }
        return false
    }
}
