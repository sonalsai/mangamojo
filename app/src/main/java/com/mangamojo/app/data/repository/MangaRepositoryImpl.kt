package com.mangamojo.app.data.repository

import com.mangamojo.app.core.CachePolicy
import com.mangamojo.app.data.local.dao.FavoriteDao
import com.mangamojo.app.data.local.dao.HistoryDao
import com.mangamojo.app.data.local.dao.MangaCacheDao
import com.mangamojo.app.data.local.mapper.toCacheEntity
import com.mangamojo.app.data.local.mapper.toDetails
import com.mangamojo.app.data.local.mapper.toDomain
import com.mangamojo.app.domain.model.Chapter
import com.mangamojo.app.domain.model.MangaCategory
import com.mangamojo.app.domain.model.MangaDetails
import com.mangamojo.app.domain.model.Page
import com.mangamojo.app.domain.model.SearchQuery
import com.mangamojo.app.domain.model.SearchResult
import com.mangamojo.app.domain.model.SearchSort
import com.mangamojo.app.domain.provider.MangaProvider
import com.mangamojo.app.domain.repository.MangaRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinates the active [MangaProvider] with the local Room cache. Details and
 * chapter lists are read-through cached so previously-viewed manga stay
 * available offline, and the cache is used as a fallback when the network
 * fails. (Search results are intentionally not cached — they're query-specific
 * and short-lived.)
 */
@Singleton
class MangaRepositoryImpl @Inject constructor(
    private val provider: MangaProvider,
    private val cacheDao: MangaCacheDao,
    private val favoriteDao: FavoriteDao,
    private val historyDao: HistoryDao,
) : MangaRepository {

    override suspend fun search(query: SearchQuery): SearchResult = provider.search(query)

    override suspend fun getCategories(): List<MangaCategory> = provider.getCategories()

    override suspend fun getPopular(offset: Int, limit: Int): SearchResult =
        provider.search(SearchQuery(title = null, offset = offset, limit = limit, sort = SearchSort.POPULAR))

    override suspend fun getMangaDetails(mangaId: String, forceRefresh: Boolean): MangaDetails {
        val now = System.currentTimeMillis()
        val cached = cacheDao.getManga(mangaId)
        val fresh = cached != null && (now - cached.cachedAt) < CachePolicy.MANGA_TTL_MS
        if (cached != null && fresh && !forceRefresh) return cached.toDetails()

        return try {
            val details = provider.getMangaDetails(mangaId)
            cacheDao.upsertManga(details.toCacheEntity(now))
            details
        } catch (e: Exception) {
            cached?.toDetails() ?: throw e
        }
    }

    override suspend fun getChapters(
        mangaId: String,
        languages: List<String>,
        forceRefresh: Boolean,
    ): List<Chapter> {
        val now = System.currentTimeMillis()
        val cached = cacheDao.getChapters(mangaId)
        val newest = cached.maxOfOrNull { it.cachedAt } ?: 0L
        // Cache is keyed by manga only; a language change should force a refresh.
        val fresh = cached.isNotEmpty() && (now - newest) < CachePolicy.CHAPTERS_TTL_MS
        if (cached.isNotEmpty() && fresh && !forceRefresh) return cached.map { it.toDomain() }

        return try {
            val chapters = provider.getChapters(mangaId, languages)
            cacheDao.deleteChaptersForManga(mangaId)
            cacheDao.upsertChapters(chapters.mapIndexed { index, chapter ->
                chapter.toCacheEntity(orderIndex = index, now = now)
            })
            chapters
        } catch (e: Exception) {
            if (cached.isNotEmpty()) cached.map { it.toDomain() } else throw e
        }
    }

    override suspend fun getPages(chapterId: String, dataSaver: Boolean): List<Page> =
        provider.getPages(chapterId, dataSaver)

    override suspend fun clearCache() {
        cacheDao.clearChapters()
        cacheDao.clearManga()
    }

    override suspend fun evictStaleCache() {
        val protectedIds = (favoriteDao.getFavoriteIds() + historyDao.getHistoryMangaIds()).distinct()
        val cutoff = System.currentTimeMillis() - CachePolicy.MAX_CACHE_AGE_MS
        cacheDao.evictStaleChapters(cutoff, protectedIds)
        cacheDao.evictStaleManga(cutoff, protectedIds)
    }

    override fun observeCachedMangaCount(): Flow<Int> = cacheDao.observeMangaCount()
}
