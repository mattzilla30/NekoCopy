package org.nekomanga.domain.site

import android.util.Base64
import eu.kanade.tachiyomi.source.online.utils.MdLang
import java.security.SecureRandom
import org.nekomanga.constants.MdConstants
import tachiyomi.core.preference.Preference
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

    fun sessionToken() = this.preferenceStore.getString("mangadex_session_token")

    fun refreshToken() = this.preferenceStore.getString("mangadex_refresh_token")

    fun lastRefreshTime() = this.preferenceStore.getLong("mangadex_refresh_token_time", 0)

    fun unexpectedLogout() = this.preferenceStore.getBoolean("mangadex_unexpected_logout", false)

    fun removeTokens() {
        sessionToken().delete()
        refreshToken().delete()
        lastRefreshTime().delete()
        codeVerifier().delete()
    }

    fun setTokens(refresh: String, session: String) {
        val time = if (refresh.isBlank() && session.isBlank()) 0 else System.currentTimeMillis()
        sessionToken().set(session)
        refreshToken().set(refresh)
        lastRefreshTime().set(time)
    }

    /** Returns the PKCE code verifier, creating and storing a new one when none exists. */
    fun codeVerifier(): Preference<String> {
        val codeVerifier = this.preferenceStore.getString("mangadex_code_verifier")
        if (codeVerifier.get().isEmpty()) {
            val bytes = ByteArray(64).also { SecureRandom().nextBytes(it) }
            val encoding = Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
            codeVerifier.set(Base64.encodeToString(bytes, encoding))
        }
        return codeVerifier
    }
}
