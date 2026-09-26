package org.nekomanga.presentation.screens.settings.screens

import android.content.Context
import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import eu.kanade.tachiyomi.ui.setting.CacheData
import eu.kanade.tachiyomi.ui.setting.CacheType
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.SharedFlow
import org.nekomanga.R
import org.nekomanga.domain.storage.StoragePreferences
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.components.storage.storageLocationPicker
import org.nekomanga.presentation.components.storage.storageLocationText
import org.nekomanga.presentation.screens.settings.Preference
import org.nekomanga.presentation.screens.settings.widgets.SearchTerm
import org.nekomanga.presentation.theme.Size
import tachiyomi.core.util.storage.DiskUtil
import uy.kohesive.injekt.api.get

internal class DataStorageSettingsScreen(
    incognitoMode: Boolean,
    val storagePreferences: StoragePreferences,
    val cacheData: CacheData,
    val clearCache: (CacheType) -> Unit,
    val toastEvent: SharedFlow<UiText.StringResource>,
    onNavigationIconClick: (() -> Unit)?,
) : SearchableSettings(onNavigationIconClick, incognitoMode) {

    override fun getTitleRes(): Int = R.string.data_storage

    @Composable
    override fun getPreferences(): List<Preference> {
        val context = LocalContext.current

        LaunchedEffect(Unit) { toastEvent.collect { event -> context.toast(event.resourceId) } }

        val pickStorageLocation = storageLocationPicker(storagePreferences.baseStorageDirectory())

        return listOf(
            Preference.PreferenceItem.TextPreference(
                title = stringResource(R.string.storage_location),
                subtitle = storageLocationText(storagePreferences.baseStorageDirectory()),
                onClick = { pickStorageLocation.launch(null) },
            ),
            storageGroup(context),
            cacheGroup(context, cacheData, clearCache),
        )
    }

    @Composable
    private fun storageGroup(context: Context): Preference.PreferenceGroup {
        val storages = remember { DiskUtil.getExternalStorages(context) }

        return Preference.PreferenceGroup(
            title = stringResource(R.string.storage_usage),
            preferenceItems =
                listOf(
                    Preference.PreferenceItem.InfoPreference(
                        stringResource(R.string.storage_usage_info)
                    ),
                    Preference.PreferenceItem.CustomPreference(
                        title = "",
                        content = {
                            Column(
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .padding(horizontal = Size.medium, vertical = Size.small),
                                verticalArrangement = Arrangement.spacedBy(Size.small),
                            ) {
                                storages.forEach { file ->
                                    val available =
                                        remember(file) { DiskUtil.getAvailableStorageSpace(file) }
                                    val availableText =
                                        remember(available) {
                                            Formatter.formatFileSize(context, available)
                                        }
                                    val total =
                                        remember(file) { DiskUtil.getTotalStorageSpace(file) }
                                    val totalText =
                                        remember(total) { Formatter.formatFileSize(context, total) }
                                    val progress =
                                        if (total > 0L) (1 - (available / total.toFloat())) else 0f

                                    Column(verticalArrangement = Arrangement.spacedBy(Size.tiny)) {
                                        Text(
                                            text = file.absolutePath,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )

                                        LinearWavyProgressIndicator(
                                            progress = { progress },
                                            modifier = Modifier.fillMaxWidth(),
                                            color = MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                        )

                                        Text(
                                            text =
                                                stringResource(
                                                    R.string.available_disk_space_info,
                                                    availableText,
                                                    totalText,
                                                ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        },
                    ),
                ),
        )
    }

    @Composable
    private fun cacheGroup(
        context: Context,
        cacheData: CacheData,
        clearCache: (CacheType) -> Unit,
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(R.string.cache),
            preferenceItems =
                listOf(
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(R.string.parent_cache_folder),
                        subtitle = stringResource(R.string.used_, cacheData.parentCacheSize),
                        onClick = { clearCache(CacheType.Parent) },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(R.string.chapter_disk_cache),
                        subtitle = stringResource(R.string.used_, cacheData.chapterDiskCacheSize),
                        onClick = { clearCache(CacheType.ChapterDisk) },
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        title = stringResource(R.string.auto_clear_chapter_cache),
                        pref = storagePreferences.autoClearChapterCache(),
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(R.string.custom_cover_cache),
                        subtitle = stringResource(R.string.used_, cacheData.customCoverCacheSize),
                        onClick = { clearCache(CacheType.CustomCover) },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(R.string.online_cover_cache),
                        subtitle = stringResource(R.string.used_, cacheData.onlineCoverCacheSize),
                        onClick = { clearCache(CacheType.OnlineCover) },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(R.string.image_cache),
                        subtitle = stringResource(R.string.used_, cacheData.imageCacheSize),
                        onClick = { clearCache(CacheType.Image) },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(R.string.network_cache),
                        subtitle = stringResource(R.string.used_, cacheData.networkCacheSize),
                        onClick = { clearCache(CacheType.Network) },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(R.string.temp_file_cache),
                        subtitle = stringResource(R.string.used_, cacheData.tempFileCacheSize),
                        onClick = { clearCache(CacheType.Temp) },
                    ),
                ),
        )
    }

    companion object : SearchTermProvider {
        @Composable
        override fun getSearchTerms(): List<SearchTerm> {
            return listOf(
                SearchTerm(title = stringResource(R.string.storage_location)),
                SearchTerm(
                    title = stringResource(R.string.storage_usage),
                    group = stringResource(R.string.storage_usage),
                ),
                SearchTerm(
                    title = stringResource(R.string.parent_cache_folder),
                    group = stringResource(R.string.cache),
                ),
                SearchTerm(
                    title = stringResource(R.string.chapter_disk_cache),
                    group = stringResource(R.string.cache),
                ),
                SearchTerm(
                    title = stringResource(R.string.auto_clear_chapter_cache),
                    group = stringResource(R.string.cache),
                ),
                SearchTerm(
                    title = stringResource(R.string.custom_cover_cache),
                    group = stringResource(R.string.cache),
                ),
                SearchTerm(
                    title = stringResource(R.string.online_cover_cache),
                    group = stringResource(R.string.cache),
                ),
                SearchTerm(
                    title = stringResource(R.string.image_cache),
                    group = stringResource(R.string.cache),
                ),
                SearchTerm(
                    title = stringResource(R.string.network_cache),
                    group = stringResource(R.string.cache),
                ),
                SearchTerm(
                    title = stringResource(R.string.temp_file_cache),
                    group = stringResource(R.string.cache),
                ),
            )
        }
    }
}
