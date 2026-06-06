package com.mangamojo.app.data.repository

import com.mangamojo.app.core.CachePolicy
import com.mangamojo.app.data.local.dao.FavoriteDao
import com.mangamojo.app.data.local.dao.HistoryDao
import com.mangamojo.app.data.local.dao.ReadingProgressDao
import com.mangamojo.app.data.local.entity.FavoriteEntity
import com.mangamojo.app.data.local.entity.HistoryEntity
import com.mangamojo.app.data.local.entity.ReadingProgressEntity
import com.mangamojo.app.data.local.mapper.toDomain
import com.mangamojo.app.domain.model.Chapter
import com.mangamojo.app.domain.model.Favorite
import com.mangamojo.app.domain.model.HistoryEntry
import com.mangamojo.app.domain.model.MangaDetails
import com.mangamojo.app.domain.model.ReadingProgress
import com.mangamojo.app.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryRepositoryImpl @Inject constructor(
    private val favoriteDao: FavoriteDao,
    private val historyDao: HistoryDao,
    private val progressDao: ReadingProgressDao,
) : LibraryRepository {

    /* ----- Favorites ----- */

    override fun observeFavorites(): Flow<List<Favorite>> =
        favoriteDao.observeFavorites().map { list -> list.map { it.toDomain() } }

    override fun observeIsFavorite(mangaId: String): Flow<Boolean> =
        favoriteDao.observeIsFavorite(mangaId)

    override suspend fun toggleFavorite(manga: MangaDetails) {
        if (favoriteDao.isFavorite(manga.id)) {
            favoriteDao.delete(manga.id)
        } else {
            favoriteDao.upsert(
                FavoriteEntity(
                    mangaId = manga.id,
                    sourceId = manga.sourceId,
                    title = manga.title,
                    coverUrl = manga.coverUrl,
                    status = manga.status.raw,
                    addedAt = System.currentTimeMillis(),
                )
            )
        }
    }

    override suspend fun clearFavorites() = favoriteDao.clear()

    /* ----- History ----- */

    override fun observeHistory(limit: Int): Flow<List<HistoryEntry>> =
        historyDao.observeHistory(limit).map { list -> list.map { it.toDomain() } }

    override fun observeContinueReading(limit: Int): Flow<List<HistoryEntry>> =
        historyDao.observeContinueReading(limit).map { list -> list.map { it.toDomain() } }

    override suspend fun removeFromHistory(mangaId: String) = historyDao.delete(mangaId)

    override suspend fun clearHistory() = historyDao.clear()

    /* ----- Reading progress ----- */

    override suspend fun saveProgress(manga: MangaDetails, chapter: Chapter, page: Int, total: Int) {
        val now = System.currentTimeMillis()
        // Never regress the furthest-read page for the same chapter — guards
        // against transient page-0 writes while the reader restores scroll.
        val existing = progressDao.getByChapter(chapter.id)
        val furthest = if (existing != null && existing.total == total && existing.page > page) {
            existing.page
        } else {
            page
        }
        val completed = total > 0 && furthest >= total - 1

        progressDao.upsert(
            ReadingProgressEntity(
                chapterId = chapter.id,
                mangaId = manga.id,
                page = furthest,
                total = total,
                completed = completed,
                updatedAt = now,
            )
        )
        historyDao.upsert(
            HistoryEntity(
                mangaId = manga.id,
                sourceId = manga.sourceId,
                title = manga.title,
                coverUrl = manga.coverUrl,
                contentRating = manga.contentRating,
                chapterId = chapter.id,
                chapterLabel = chapter.label,
                page = furthest,
                total = total,
                readAt = now,
            )
        )
        historyDao.trimToLimit(CachePolicy.HISTORY_LIMIT)
    }

    override suspend fun setChapterReadState(manga: MangaDetails, chapter: Chapter, read: Boolean) {
        if (read) {
            val total = chapter.pages.takeIf { it > 0 } ?: 1
            saveProgress(manga, chapter, page = total - 1, total = total)
        } else {
            progressDao.deleteByChapter(chapter.id)
            historyDao.deleteChapter(manga.id, chapter.id)
        }
    }

    override suspend fun getChapterProgress(chapterId: String): ReadingProgress? =
        progressDao.getByChapter(chapterId)?.toDomain()

    override fun observeChapterProgress(chapterId: String): Flow<ReadingProgress?> =
        progressDao.observeByChapter(chapterId).map { it?.toDomain() }

    override fun observeMangaProgress(mangaId: String): Flow<List<ReadingProgress>> =
        progressDao.observeByManga(mangaId).map { list -> list.map { it.toDomain() } }

    override fun observeReadChapterIds(mangaId: String): Flow<Set<String>> =
        progressDao.observeReadChapterIds(mangaId).map { it.toSet() }

    override suspend fun clearProgress() = progressDao.clear()
}
