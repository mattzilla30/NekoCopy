package eu.kanade.tachiyomi.data.track.anilist

import androidx.core.net.toUri
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.util.system.withIOContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.nekomanga.constants.Constants
import org.nekomanga.core.network.POST
import org.nekomanga.core.network.interceptor.rateLimit
import org.nekomanga.logging.TimberKt
import tachiyomi.core.network.await
import tachiyomi.core.network.jsonMime
import tachiyomi.core.network.parseAs
import uy.kohesive.injekt.injectLazy

class AnilistApi(val client: OkHttpClient, interceptor: AnilistInterceptor) {
    private val json: Json by injectLazy()

    private val authClient =
        client
            .newBuilder()
            .addInterceptor(interceptor)
            .rateLimit(3, period = 10, unit = TimeUnit.SECONDS)
            .build()

    /** Sends a GraphQL [payload] to AniList and parses the JSON response. */
    private suspend fun postQuery(payload: JsonObject): JsonObject =
        with(json) {
            authClient
                .newCall(POST(apiUrl, body = payload.toString().toRequestBody(jsonMime)))
                .await()
                .parseAs<JsonObject>()
        }

    /** Adds or updates the list entry for [track], passing its progress, status and dates. */
    private fun listEntryPayload(query: String, track: Track, extra: JsonObjectBuilder.() -> Unit) =
        buildJsonObject {
            put("query", query)
            putJsonObject("variables") {
                extra()
                put("progress", track.last_chapter_read.toInt())
                put("status", track.toAnilistStatus())
                createDate(track.started_reading_date)?.let { date ->
                    put("startedAt", Json.encodeToJsonElement(date))
                }
                createDate(track.finished_reading_date)?.let { date ->
                    put("completedAt", Json.encodeToJsonElement(date))
                }
            }
        }

    suspend fun addLibManga(track: Track): Track {
        return withIOContext {
            val payload =
                listEntryPayload(addToLibraryQuery(), track) { put("mangaId", track.media_id) }
            val entry =
                postQuery(payload)["data"]?.jsonObject?.get("SaveMediaListEntry")?.jsonObject
            track.library_id = entry?.get("id")?.jsonPrimitive?.long ?: 0L
            track
        }
    }

    suspend fun updateLibraryManga(track: Track): Track {
        return withIOContext {
            val payload =
                listEntryPayload(updateInLibraryQuery(), track) {
                    put("listId", track.library_id)
                    put("score", track.score.toInt())
                }
            val entry =
                postQuery(payload)["data"]?.jsonObject?.get("SaveMediaListEntry")?.jsonObject
            if (entry != null) {
                val startedDate = parseDate(entry, "startedAt")
                if (track.started_reading_date <= 0L || startedDate > 0) {
                    track.started_reading_date = startedDate
                }
                val finishedDate = parseDate(entry, "completedAt")
                if (track.finished_reading_date <= 0L || finishedDate > 0) {
                    track.finished_reading_date = finishedDate
                }
            }
            track
        }
    }

    /**
     * Searches AniList. A manga with a known AniList id that was not tracked before is looked up by
     * that id first, and the title search runs when the lookup finds nothing.
     */
    suspend fun search(
        search: String,
        manga: Manga,
        wasPreviouslyTracked: Boolean,
    ): List<TrackSearch> {
        return withIOContext {
            val anilistId = manga.anilist_id?.toIntOrNull()
            if (anilistId != null && !wasPreviouslyTracked) {
                try {
                    val byId = searchMedia(findQuery()) { put("query", anilistId) }
                    if (byId.isNotEmpty()) return@withIOContext byId
                } catch (e: Exception) {
                    TimberKt.e(e) { "Error searching by Anilist ID" }
                }
            }
            searchMedia(searchQuery()) { put("query", search) }
        }
    }

    private suspend fun searchMedia(
        query: String,
        variables: JsonObjectBuilder.() -> Unit,
    ): List<TrackSearch> {
        val payload = buildJsonObject {
            put("query", query)
            putJsonObject("variables", variables)
        }
        val media =
            postQuery(payload)["data"]?.jsonObject?.get("Page")?.jsonObject?.get("media")?.jsonArray
                ?: return emptyList()
        return media
            .mapNotNull { runCatching { jsonToALManga(it.jsonObject) }.getOrNull() }
            .map { it.toTrack() }
    }

    suspend fun findLibManga(track: Track, userid: Int): Track? {
        return withIOContext {
            val payload = buildJsonObject {
                put("query", findLibraryMangaQuery())
                putJsonObject("variables") {
                    put("id", userid)
                    put("manga_id", track.media_id)
                }
            }
            val entries =
                postQuery(payload)["data"]
                    ?.jsonObject
                    ?.get("Page")
                    ?.jsonObject
                    ?.get("mediaList")
                    ?.jsonArray ?: return@withIOContext null
            entries
                .firstNotNullOfOrNull {
                    runCatching { jsonToALUserManga(it.jsonObject) }.getOrNull()
                }
                ?.toTrack()
        }
    }

    suspend fun getLibManga(track: Track, userid: Int): Track {
        return findLibManga(track, userid) ?: throw Exception("Could not find manga")
    }

    suspend fun remove(track: Track): Boolean {
        return withIOContext {
            val payload = buildJsonObject {
                put("query", deleteFromLibraryQuery())
                putJsonObject("variables") { put("listId", track.library_id) }
            }
            authClient
                .newCall(POST(apiUrl, body = payload.toString().toRequestBody(jsonMime)))
                .await()
                .isSuccessful
        }
    }

    fun createOAuth(token: String): OAuth {
        val yearToMS = TimeUnit.DAYS.toMillis(365)
        return OAuth(token, "Bearer", System.currentTimeMillis() + yearToMS, yearToMS)
    }

    suspend fun getCurrentUser(): Pair<String, String> {
        return withIOContext {
            val payload = buildJsonObject { put("query", currentUserQuery()) }
            val data = postQuery(payload)["data"]?.jsonObject ?: throw Exception("Invalid response")
            val viewer = data["Viewer"]?.jsonObject ?: throw Exception("Viewer data not found")
            val id =
                viewer["id"]?.jsonPrimitive?.int?.toString() ?: throw Exception("User ID not found")
            val user =
                viewer["name"]?.jsonPrimitive?.content ?: throw Exception("Username not found")
            val scoreFormat =
                viewer["mediaListOptions"]?.jsonObject?.get("scoreFormat")?.jsonPrimitive?.content
                    ?: throw Exception("Score format not found")
            Pair(user + Constants.SEPARATOR + id, scoreFormat)
        }
    }

    private fun jsonToALManga(struct: JsonObject): ALManga {
        return ALManga(
            struct["id"]?.jsonPrimitive?.long ?: 0L,
            struct["title"]?.jsonObject?.get("userPreferred")?.jsonPrimitive?.content ?: "",
            struct["coverImage"]?.jsonObject?.get("large")?.jsonPrimitive?.content ?: "",
            struct["description"]?.jsonPrimitive?.contentOrNull,
            struct["format"]?.jsonPrimitive?.content?.replace("_", "-") ?: "",
            struct["status"]?.jsonPrimitive?.contentOrNull ?: "",
            parseDate(struct, "startDate"),
            struct["chapters"]?.jsonPrimitive?.intOrNull ?: 0,
        )
    }

    private fun jsonToALUserManga(struct: JsonObject): ALUserManga {
        return ALUserManga(
            struct["id"]?.jsonPrimitive?.long ?: 0L,
            struct["status"]?.jsonPrimitive?.content ?: "",
            struct["scoreRaw"]?.jsonPrimitive?.int ?: 0,
            struct["progress"]?.jsonPrimitive?.int ?: 0,
            parseDate(struct, "startedAt"),
            parseDate(struct, "completedAt"),
            struct["media"]?.jsonObject?.let { jsonToALManga(it) }
                ?: ALManga(0L, "", "", null, "", "", 0L, 0),
        )
    }

    private fun parseDate(struct: JsonObject, dateKey: String): Long {
        return try {
            val date = Calendar.getInstance()
            val dateObj = struct[dateKey]?.jsonObject ?: return 0L
            val year = dateObj["year"]?.jsonPrimitive?.int ?: return 0L
            val month = dateObj["month"]?.jsonPrimitive?.int?.minus(1) ?: return 0L
            val day = dateObj["day"]?.jsonPrimitive?.int ?: return 0L
            date.set(year, month, day)
            date.timeInMillis
        } catch (_: Exception) {
            0L
        }
    }

    private fun createDate(dateValue: Long): AniListDate? {
        if (dateValue == -1L) return AniListDate()
        if (dateValue == 0L) return null
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = dateValue
        return calendar.toAniList()
    }

    private fun Calendar.toAniList(): AniListDate {
        return AniListDate(get(Calendar.YEAR), get(Calendar.MONTH) + 1, get(Calendar.DAY_OF_MONTH))
    }

    @Serializable
    private data class AniListDate(
        val year: Int? = null,
        val month: Int? = null,
        val day: Int? = null,
    )

    companion object {
        private const val clientId = "1984"
        private const val apiUrl = "https://graphql.anilist.co/"
        private const val baseUrl = "https://anilist.co/api/v2/"
        private const val baseMangaUrl = "https://anilist.co/manga/"

        fun mangaUrl(mediaId: Long): String {
            return baseMangaUrl + mediaId
        }

        fun authUrl() =
            "${baseUrl.toUri()}oauth/authorize"
                .toUri()
                .buildUpon()
                .appendQueryParameter("client_id", clientId)
                .appendQueryParameter("response_type", "token")
                .build()!!

        fun addToLibraryQuery() =
            """
            |mutation AddManga(${'$'}mangaId: Int, ${'$'}progress: Int, ${'$'}status: MediaListStatus, ${'$'}startedAt: FuzzyDateInput, ${'$'}completedAt: FuzzyDateInput) {
            |SaveMediaListEntry (mediaId: ${'$'}mangaId, progress: ${'$'}progress, status: ${'$'}status, startedAt: ${'$'}startedAt, completedAt: ${'$'}completedAt) {
            |   id
            |   status
            |}
            |}
            |"""
                .trimMargin()

        fun deleteFromLibraryQuery() =
            """
            |mutation DeleteManga(${'$'}listId: Int) {
            |DeleteMediaListEntry (id: ${'$'}listId) {
            |deleted
            |
            |}
            |}
            """
                .trimMargin()

        fun updateInLibraryQuery() =
            """
            |mutation UpdateManga(${'$'}listId: Int, ${'$'}progress: Int, ${'$'}status: MediaListStatus, ${'$'}score: Int, ${'$'}startedAt: FuzzyDateInput, ${'$'}completedAt: FuzzyDateInput) {
            |SaveMediaListEntry (id: ${'$'}listId, progress: ${'$'}progress, status: ${'$'}status, scoreRaw: ${'$'}score, startedAt: ${'$'}startedAt, completedAt: ${'$'}completedAt) {
            |id
            |status
            |progress
            |startedAt {
            |year
            |month
            |day
            |}
            |completedAt {
            |year
            |month
            |day
            |}
            |}
            |}
            |"""
                .trimMargin()

        /** The manga fields every media query asks for. */
        private const val MEDIA_FIELDS =
            """
            id
            title { userPreferred }
            coverImage { large }
            format
            status
            chapters
            description
            startDate { year month day }
            """

        fun searchQuery() =
            """
            query Search(${'$'}query: String) {
              Page(perPage: 50) {
                media(search: ${'$'}query, type: MANGA, format_not_in: [NOVEL]) { $MEDIA_FIELDS }
              }
            }
            """

        fun findQuery() =
            """
            query Media(${'$'}query: Int) {
              Page(perPage: 50) {
                media(id: ${'$'}query, type: MANGA, format_not_in: [NOVEL]) { $MEDIA_FIELDS }
              }
            }
            """

        fun findLibraryMangaQuery() =
            """
            query (${'$'}id: Int!, ${'$'}manga_id: Int!) {
              Page {
                mediaList(userId: ${'$'}id, type: MANGA, mediaId: ${'$'}manga_id) {
                  id
                  status
                  scoreRaw: score(format: POINT_100)
                  progress
                  startedAt { year month day }
                  completedAt { year month day }
                  media { $MEDIA_FIELDS }
                }
              }
            }
            """

        fun currentUserQuery() =
            """
            |query User {
            |Viewer {
            |id
            |name
            |mediaListOptions {
            |scoreFormat
            |}
            |}
            |}
            |"""
                .trimMargin()
    }
}
