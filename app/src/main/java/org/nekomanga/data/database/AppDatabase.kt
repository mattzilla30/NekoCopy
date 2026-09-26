package org.nekomanga.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.nekomanga.data.database.dao.ArtworkDao
import org.nekomanga.data.database.dao.BrowseFilterDao
import org.nekomanga.data.database.dao.ChapterDao
import org.nekomanga.data.database.dao.HistoryDao
import org.nekomanga.data.database.dao.MangaAggregateDao
import org.nekomanga.data.database.dao.MangaDao
import org.nekomanga.data.database.dao.ScanlatorGroupDao
import org.nekomanga.data.database.dao.SimilarDao
import org.nekomanga.data.database.dao.UploaderDao
import org.nekomanga.data.database.entity.ArtworkEntity
import org.nekomanga.data.database.entity.BrowseFilterEntity
import org.nekomanga.data.database.entity.ChapterEntity
import org.nekomanga.data.database.entity.HistoryEntity
import org.nekomanga.data.database.entity.MangaAggregateEntity
import org.nekomanga.data.database.entity.MangaEntity
import org.nekomanga.data.database.entity.MangaSimilarEntity
import org.nekomanga.data.database.entity.ScanlatorGroupEntity
import org.nekomanga.data.database.entity.UploaderEntity
import org.nekomanga.data.database.utils.FollowStatusConverter

@Database(
    entities =
        [
            MangaEntity::class,
            ChapterEntity::class,
            ArtworkEntity::class,
            BrowseFilterEntity::class,
            HistoryEntity::class,
            MangaAggregateEntity::class,
            ScanlatorGroupEntity::class,
            MangaSimilarEntity::class,
            UploaderEntity::class,
        ],
    version = 49,
    exportSchema = true,
)
@TypeConverters(FollowStatusConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun artworkDao(): ArtworkDao

    abstract fun browseFilterDao(): BrowseFilterDao

    abstract fun chapterDao(): ChapterDao

    abstract fun historyDao(): HistoryDao

    abstract fun mangaAggregateDao(): MangaAggregateDao

    abstract fun mangaDao(): MangaDao

    abstract fun scanlatorGroupDao(): ScanlatorGroupDao

    abstract fun similarDao(): SimilarDao

    abstract fun uploaderDao(): UploaderDao

    companion object {
        const val DATABASE_NAME = "tachiyomi.db"
    }
}
