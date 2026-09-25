package org.nekomanga.data.database.repository

import eu.kanade.tachiyomi.data.database.models.MangaAggregate
import org.nekomanga.data.database.dao.MangaAggregateDao
import org.nekomanga.data.database.mapper.toEntity
import org.nekomanga.data.database.mapper.toMangaAggregate

class MangaAggregateRepositoryImpl(private val mangaAggregateDao: MangaAggregateDao) :
    MangaAggregateRepository {

    override suspend fun getMangaAggregate(mangaId: Long): MangaAggregate? {
        return mangaAggregateDao.getMangaAggregateSync(mangaId)?.toMangaAggregate()
    }

    override suspend fun insertMangaAggregate(aggregate: MangaAggregate) {
        mangaAggregateDao.insertMangaAggregate(aggregate.toEntity())
    }

    override suspend fun deleteMangaAggregate(mangaId: Long) {
        mangaAggregateDao.deleteMangaAggregate(mangaId)
    }
}
