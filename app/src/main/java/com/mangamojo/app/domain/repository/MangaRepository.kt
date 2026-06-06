package com.mangamojo.app.domain.repository

import com.mangamojo.app.domain.model.Chapter
import com.mangamojo.app.domain.model.MangaCategory
import com.mangamojo.app.domain.model.MangaDetails
import com.mangamojo.app.domain.model.Page
import com.mangamojo.app.domain.model.SearchQuery
import com.mangamojo.app.domain.model.SearchResult
import kotlinx.coroutines.flow.Flow

/**
 * Catalogue access: searching, details, chapters and pages. Implementations
 * coordinate the provider(s) with the local cache (Room) so the app stays
 * usable offline for previously-viewed content.
 */
interface MangaRepository {

    suspend fun search(query: SearchQuery): SearchResult

    suspend fun getCategories(): List<MangaCategory>

    suspend fun getPopular(offset: Int, limit: Int): SearchResult

    /** Returns cached details immediately when fresh; otherwise fetches and
     *  caches. Falls back to stale cache if the network fails. */
    suspend fun getMangaDetails(mangaId: String, forceRefresh: Boolean = false): MangaDetails

    suspend fun getChapters(
        mangaId: String,
        languages: List<String>,
        forceRefresh: Boolean = false,
    ): List<Chapter>

    suspend fun getPages(chapterId: String, dataSaver: Boolean): List<Page>

    /* ----- Cache maintenance (used by Settings + the cleanup worker) ----- */

    /** Wipe all cached metadata. Favorites/history/progress are untouched. */
    suspend fun clearCache()

    /** Evict cached manga/chapters older than the policy TTL, preserving any
     *  that are favorited or appear in recent history. */
    suspend fun evictStaleCache()

    fun observeCachedMangaCount(): Flow<Int>
}
