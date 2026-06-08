package com.mangamojo.app.domain.provider

import com.mangamojo.app.domain.model.Chapter
import com.mangamojo.app.domain.model.MangaCategory
import com.mangamojo.app.domain.model.MangaDetails
import com.mangamojo.app.domain.model.Page
import com.mangamojo.app.domain.model.SearchQuery
import com.mangamojo.app.domain.model.SearchResult

/**
 * The single seam every manga source plugs into. Phase 1 implements exactly one
 * ([com.mangamojo.app.providers.mangadex.MangaDexProvider]); Phase 2 can add
 * more behind the same contract and merge/deduplicate at the repository layer
 * without touching UI or domain code.
 *
 * This is the evolved form of the Phase 1 sketch: it adds an [id]/[name] for
 * source attribution, a configurable base URL, provider health checks,
 * paginated/typed search, and richer return types so the UI never sees
 * source-specific DTOs.
 *
 * All methods are suspend and may throw; the repository/use-case layer is
 * responsible for catching and normalizing failures into
 * [com.mangamojo.app.core.AppError].
 */
interface MangaProvider {
    /** Stable identifier, e.g. "mangadex". */
    val id: String

    /** Human-readable name for UI attribution. */
    val name: String

    /** Provider root URL, used for attribution and health checks. */
    val baseUrl: String

    suspend fun search(query: SearchQuery): SearchResult

    suspend fun getCategories(): List<MangaCategory>

    suspend fun getMangaDetails(mangaId: String): MangaDetails

    suspend fun getChapters(mangaId: String, languages: List<String>): List<Chapter>

    suspend fun getPages(chapterId: String, dataSaver: Boolean): List<Page>

    suspend fun isAvailable(): Boolean = true
}
