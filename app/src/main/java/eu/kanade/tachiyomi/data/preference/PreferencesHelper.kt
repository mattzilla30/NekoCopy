package eu.kanade.tachiyomi.data.preference

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys
import eu.kanade.tachiyomi.ui.main.states.SideNavAlignment
import eu.kanade.tachiyomi.ui.main.states.SideNavMode
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale
import org.nekomanga.domain.chapter.ScanlatorFilterOption
import org.nekomanga.presentation.screens.feed.FeedHistoryGroup
import org.nekomanga.presentation.theme.Themes
import tachiyomi.core.preference.Preference
import tachiyomi.core.preference.PreferenceStore
import tachiyomi.core.preference.getEnum

operator fun <T> Preference<Set<T>>.plusAssign(item: T) {
    set(get() + item)
}

operator fun <T> Preference<Set<T>>.minusAssign(item: T) {
    set(get() - item)
}

operator fun <T> Preference<Set<T>>.plusAssign(item: Collection<T>) {
    set(get() + item)
}

operator fun <T> Preference<Set<T>>.minusAssign(item: Collection<T>) {
    set(get() - item)
}

class PreferencesHelper(val context: Context, val preferenceStore: PreferenceStore) {

    fun getInt(key: String, default: Int) = this.preferenceStore.getInt(key, default)

    fun startingTab() = this.preferenceStore.getInt(Keys.startingTab, 1)

    fun lastUsedStartingTab() = this.preferenceStore.getInt("last_used_tab", 1)

    fun hasShownOnboarding() =
        preferenceStore.getBoolean(Preference.appStateKey("onboarding_complete"), false)

    fun nightMode() =
        this.preferenceStore.getInt(Keys.nightMode, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

    fun lightTheme() =
        this.preferenceStore.getEnum(
            Keys.lightTheme,
            Themes.Kitty,
        )

    fun darkTheme() =
        this.preferenceStore.getEnum(
            Keys.darkTheme,
            Themes.Kitty,
        )

    fun useVividColorHeaders() = this.preferenceStore.getBoolean("vivid_color_headers", true)

    fun showNavigationOverlayNewUser() =
        this.preferenceStore.getBoolean(Keys.showNavigationOverlayNewUser, true)

    fun showNavigationOverlayNewUserWebtoon() =
        this.preferenceStore.getBoolean(Keys.showNavigationOverlayNewUserWebtoon, true)

    fun lastVersionCode() = this.preferenceStore.getInt("last_version_code", 0)

    fun browseAsList() = this.preferenceStore.getBoolean(Keys.catalogueAsList, false)

    fun dateFormat(
        format: String = this.preferenceStore.getString(Keys.dateFormat, "").get()
    ): DateFormat =
        when (format) {
            "" -> DateFormat.getDateInstance(DateFormat.SHORT)
            else -> SimpleDateFormat(format, Locale.getDefault())
        }

    fun dateFormatPreference() = this.preferenceStore.getString(Keys.dateFormat, "")

    fun openLinksInBrowser() = this.preferenceStore.getBoolean(Keys.openLinksInBrowser, false)

    fun removeAfterReadSlots() = this.preferenceStore.getInt(Keys.removeAfterReadSlots, -1)

    fun groupChaptersUpdates() = this.preferenceStore.getBoolean(Keys.groupChaptersUpdates, false)

    fun historyChapterGrouping() =
        preferenceStore.getEnum(Keys.historyChapterGrouping, FeedHistoryGroup.Series)

    fun feedViewOutlineCards() = preferenceStore.getBoolean("feedViewCardOutlined", false)

    fun deleteRemovedChapters() = this.preferenceStore.getInt(Keys.deleteRemovedChapters, 0)

    fun sideNavIconAlignment() =
        this.preferenceStore.getObjectFromInt(
            key = "pref_side_nav_icon_alignment",
            defaultValue = SideNavAlignment.Center,
            serializer = SideNavAlignment::toInt,
            deserializer = SideNavAlignment::fromInt,
        )

    fun showSeriesInShortcuts() = this.preferenceStore.getBoolean(Keys.showSeriesInShortcuts, true)

    fun openChapterInShortcuts() =
        this.preferenceStore.getBoolean(Keys.openChapterInShortcuts, true)

    fun sideNavMode() =
        this.preferenceStore.getObjectFromInt(
            key = "side_nav_mode",
            defaultValue = SideNavMode.Default,
            serializer = SideNavMode::toInt,
            deserializer = SideNavMode::fromInt,
        )

    fun outlineOnCovers() = this.preferenceStore.getBoolean("outline_on_covers", true)

    fun chapterScanlatorFilterOption() =
        this.preferenceStore.getObjectFromInt(
            key = "chapter_scanlator_filter_option",
            defaultValue = ScanlatorFilterOption.ANY,
            serializer = { it.value },
            deserializer = { i -> ScanlatorFilterOption.fromInt(i) },
        )
}
