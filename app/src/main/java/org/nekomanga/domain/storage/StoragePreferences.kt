package org.nekomanga.domain.storage

import android.content.Context
import tachiyomi.core.preference.PreferenceStore
import tachiyomi.core.util.storage.FolderProvider

class StoragePreferences(
    private val context: Context,
    private val folderProvider: FolderProvider,
    private val preferenceStore: PreferenceStore,
) {

    fun baseStorageDirectory() = preferenceStore.getString("storage_dir", folderProvider.path())

    fun autoClearChapterCache() = preferenceStore.getBoolean("auto_clear_chapter_cache", false)
}
