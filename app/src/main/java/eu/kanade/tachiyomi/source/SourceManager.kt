package eu.kanade.tachiyomi.source

import eu.kanade.tachiyomi.source.online.MangaDex

/** Currently hardcoded to always return the same English [MangaDex] instance */
open class SourceManager {

    val mangaDex: MangaDex = MangaDex()

    open fun get(sourceKey: Long): Source? {
        return mangaDex
    }
}
