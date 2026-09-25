package org.nekomanga.data.database.repository

import eu.kanade.tachiyomi.data.database.models.MangaAggregate

interface MangaAggregateRepository {

    suspend fun getMangaAggregate(mangaId: Long): MangaAggregate?

    suspend fun insertMangaAggregate(aggregate: MangaAggregate)

    suspend fun deleteMangaAggregate(mangaId: Long)
}
