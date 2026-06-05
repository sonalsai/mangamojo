package com.mangamojo.app.domain.repository

import com.mangamojo.app.domain.model.Chapter
import com.mangamojo.app.domain.model.Favorite
import com.mangamojo.app.domain.model.HistoryEntry
import com.mangamojo.app.domain.model.MangaDetails
import com.mangamojo.app.domain.model.ReadingProgress
import kotlinx.coroutines.flow.Flow

/**
 * All local, user-owned data: favorites, history, reading progress. Everything
 * reactive is exposed as [Flow] so screens recompose automatically.
 */
interface LibraryRepository {

    /* ----- Favorites ----- */
    fun observeFavorites(): Flow<List<Favorite>>
    fun observeIsFavorite(mangaId: String): Flow<Boolean>
    suspend fun toggleFavorite(manga: MangaDetails)
    suspend fun clearFavorites()

    /* ----- History ----- */
    fun observeHistory(limit: Int): Flow<List<HistoryEntry>>
    fun observeContinueReading(limit: Int): Flow<List<HistoryEntry>>
    suspend fun removeFromHistory(mangaId: String)
    suspend fun clearHistory()

    /* ----- Reading progress ----- */
    suspend fun saveProgress(manga: MangaDetails, chapter: Chapter, page: Int, total: Int)
    suspend fun setChapterReadState(manga: MangaDetails, chapter: Chapter, read: Boolean)
    suspend fun getChapterProgress(chapterId: String): ReadingProgress?
    fun observeChapterProgress(chapterId: String): Flow<ReadingProgress?>
    fun observeMangaProgress(mangaId: String): Flow<List<ReadingProgress>>
    fun observeReadChapterIds(mangaId: String): Flow<Set<String>>
    suspend fun clearProgress()
}
