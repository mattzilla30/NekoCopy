package org.nekomanga.domain.site

import eu.kanade.tachiyomi.source.online.utils.MdLang
import org.nekomanga.constants.MdConstants
import tachiyomi.core.preference.PreferenceStore

class MangaDexPreferences(private val preferenceStore: PreferenceStore) {

    fun visibleContentRatings() =
        this.preferenceStore.getStringSet(
            "content_rating_options",
            setOf(MdConstants.ContentRating.safe, MdConstants.ContentRating.suggestive),
        )

    fun showContentRatingFilter() = this.preferenceStore.getBoolean("show_R18_filter", true)

    fun dataSaver() = this.preferenceStore.getBoolean("data_saver_bool", false)

    fun usePort443ForImageServer() =
        this.preferenceStore.getBoolean("use_port_443_only_for_image_server", true)

    fun coverQuality() = this.preferenceStore.getInt("mangadex_thumbnail_quality", 0)

    fun includeUnavailableChapters() = this.preferenceStore.getBoolean("include_unavailable")

    fun enabledChapterLanguages() =
        this.preferenceStore.getStringSet("mangadex_chapter_languages", setOf(MdLang.ENGLISH.lang))

    fun blockedGroups() = this.preferenceStore.getStringSet("blocked_scanlators", emptySet())

    fun blockedUploaders() = this.preferenceStore.getStringSet("blocked_uploaders", emptySet())
}
