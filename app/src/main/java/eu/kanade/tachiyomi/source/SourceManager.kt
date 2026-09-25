package eu.kanade.tachiyomi.source

import eu.kanade.tachiyomi.source.online.MangaDex
import eu.kanade.tachiyomi.source.online.utils.MdLang
import java.security.MessageDigest

/** Currently hardcoded to always return the same English [MangaDex] instance */
open class SourceManager {

    val mangaDex: MangaDex = MangaDex()

    open fun get(sourceKey: Long): Source? {
        return mangaDex
    }

    fun isMangadex(sourceKey: Long): Boolean {
        return possibleIds.contains(sourceKey)
    }

    companion object {

        val possibleIds = MdLang.entries.map { getId(it.lang) }

        fun getId(lang: String): Long {
            val key = "mangadex/$lang/1"
            val bytes = MessageDigest.getInstance("MD5").digest(key.toByteArray())
            return (0..7).map { bytes[it].toLong() and 0xff shl 8 * (7 - it) }.reduce(Long::or) and
                Long.MAX_VALUE
        }
    }
}
