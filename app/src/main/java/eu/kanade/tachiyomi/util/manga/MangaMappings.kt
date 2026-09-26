package eu.kanade.tachiyomi.util.manga

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDoneException
import java.io.File
import java.io.IOException

/**
 * Maps MangaDex ids to tracker ids and back, from a read-only database shipped in the assets.
 *
 * The table is keyed by the MangaDex UUID stored as a 16 byte blob, so lookups by MangaDex id use
 * the primary key. Tracker ids are stored as integers except for legacy MangaUpdates ids. Reverse
 * lookups scan the table, so callers with many ids use [getMangadexUUIDs] to scan once.
 */
class MangaMappings(context: Context) {

    private val dbMappings: SQLiteDatabase by lazy { openDatabase(context) }

    private fun openDatabase(context: Context): SQLiteDatabase {
        val dbFile: File = context.getDatabasePath(DB_NAME)
        if (!dbFile.exists()) {
            try {
                deleteOldDatabases(dbFile.parentFile)
                copyDatabase(context, dbFile)
            } catch (e: IOException) {
                throw RuntimeException("Error creating source database", e)
            }
        }
        return SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY)
    }

    /** Removes mapping databases copied by earlier versions. */
    private fun deleteOldDatabases(dir: File?) {
        dir?.listFiles { file -> file.name != DB_NAME && file.name.contains("mapping") }
            ?.forEach { it.delete() }
    }

    @Throws(IOException::class)
    private fun copyDatabase(context: Context, dbFile: File) {
        dbFile.parentFile?.mkdirs()
        val tempFile = File(dbFile.path + ".tmp")
        context.assets.open(DB_NAME).use { input ->
            tempFile.outputStream().use { output -> input.copyTo(output) }
        }
        // Rename once the copy finishes, so an interrupted copy never leaves a broken database.
        if (!tempFile.renameTo(dbFile)) throw IOException("Could not move $tempFile to $dbFile")
    }

    fun getMangadexUUID(id: String, service: String): String? {
        val column = column(service) ?: return null
        return queryString(
                "SELECT hex(mdex) FROM mappings WHERE $column = ? ORDER BY mdex LIMIT 1",
                id,
            )
            ?.let(::hexToUuid)
    }

    /**
     * Looks up the MangaDex UUID for each tracker id in one pass over the table. Ids without a
     * mapping are absent from the result. When several manga share a tracker id, the lowest
     * MangaDex UUID wins, matching [getMangadexUUID].
     */
    fun getMangadexUUIDs(ids: Collection<String>, service: String): Map<String, String> {
        val column = column(service) ?: return emptyMap()
        if (!dbMappings.isOpen) return emptyMap()
        val result = mutableMapOf<String, String>()
        ids.distinct().chunked(MAX_QUERY_ARGS).forEach { chunk ->
            val placeholders = chunk.joinToString(",") { "?" }
            dbMappings
                .rawQuery(
                    "SELECT $column, hex(mdex) FROM mappings WHERE $column IN ($placeholders) ORDER BY mdex",
                    chunk.toTypedArray(),
                )
                .use { cursor ->
                    while (cursor.moveToNext()) {
                        val id = cursor.getString(0) ?: continue
                        val uuid = hexToUuid(cursor.getString(1) ?: continue)
                        result.putIfAbsent(id, uuid)
                    }
                }
        }
        return result
    }

    fun getExternalID(id: String, service: String): String? {
        val column = column(service) ?: return null
        return queryByMangadexId("SELECT $column FROM mappings WHERE mdex = ?", id)
    }

    fun getMbId(id: String): String? =
        queryByMangadexId("SELECT mb FROM mappings WHERE mdex = ?", id)

    private fun queryString(query: String, arg: String): String? {
        if (!dbMappings.isOpen) return null
        return dbMappings.rawQuery(query, arrayOf(arg)).use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0)?.ifBlank { null } else null
        }
    }

    private fun queryByMangadexId(query: String, uuid: String): String? {
        val key = uuidToBytes(uuid) ?: return null
        if (!dbMappings.isOpen) return null
        return dbMappings.compileStatement(query).use { statement ->
            statement.bindBlob(1, key)
            try {
                statement.simpleQueryForString()?.ifBlank { null }
            } catch (_: SQLiteDoneException) {
                null
            }
        }
    }

    companion object {
        private const val DB_NAME = "mappings_2026-07-23.db"

        /** Stays under SQLite's default limit of 999 bound arguments per statement. */
        private const val MAX_QUERY_ARGS = 500

        private val columns = setOf("al", "mal", "mu", "mu_new", "mb")

        private fun column(service: String): String? = service.lowercase().takeIf { it in columns }

        private val uuidRegex = Regex("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}")

        internal fun uuidToBytes(uuid: String): ByteArray? {
            if (!uuidRegex.matches(uuid)) return null
            val hex = uuid.replace("-", "")
            return ByteArray(16) { i -> hex.substring(i * 2, i * 2 + 2).toInt(16).toByte() }
        }

        internal fun hexToUuid(hex: String): String {
            val lower = hex.lowercase()
            return "${lower.substring(0, 8)}-${lower.substring(8, 12)}-${lower.substring(12, 16)}-" +
                "${lower.substring(16, 20)}-${lower.substring(20)}"
        }
    }
}
