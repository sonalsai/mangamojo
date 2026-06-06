package com.mangamojo.app.providers.mangadex

import com.mangamojo.app.core.MangaDex
import com.mangamojo.app.core.SOURCE_MANGADEX
import com.mangamojo.app.data.remote.MangaDexApi
import com.mangamojo.app.data.remote.mapper.dedupeChapters
import com.mangamojo.app.data.remote.mapper.toCategory
import com.mangamojo.app.data.remote.mapper.toDetails
import com.mangamojo.app.data.remote.mapper.toDomain
import com.mangamojo.app.data.remote.mapper.toPages
import com.mangamojo.app.data.remote.mapper.toSummary
import com.mangamojo.app.domain.model.Chapter
import com.mangamojo.app.domain.model.MangaCategory
import com.mangamojo.app.domain.model.MangaDetails
import com.mangamojo.app.domain.model.Page
import com.mangamojo.app.domain.model.SearchQuery
import com.mangamojo.app.domain.model.SearchResult
import com.mangamojo.app.domain.provider.MangaProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The only [MangaProvider] in Phase 1. It owns every MangaDex-specific detail —
 * endpoint shapes, query conventions, the @Home page-delivery flow — and hands
 * back only normalized domain models. Adding another source later means adding
 * a sibling class here; nothing upstream changes.
 */
@Singleton
class MangaDexProvider @Inject constructor(
    private val api: MangaDexApi,
) : MangaProvider {

    override val id: String = SOURCE_MANGADEX
    override val name: String = "MangaDex"

    override suspend fun search(query: SearchQuery): SearchResult {
        // `relevance` is only valid alongside a title search; fall back to
        // popularity for empty-title browses.
        val sort = if (query.title.isNullOrBlank() && query.sort.name == "RELEVANCE") {
            com.mangamojo.app.domain.model.SearchSort.POPULAR
        } else {
            query.sort
        }
        val response = api.searchManga(
            title = query.title?.takeIf { it.isNotBlank() },
            limit = query.limit,
            offset = query.offset,
            contentRating = query.contentRatings,
            includes = MangaDex.MANGA_INCLUDES,
            hasAvailableChapters = "true",
            includedTags = query.includedTagIds.ifEmpty { null },
            includedTagsMode = if (query.includedTagIds.isEmpty()) null else "AND",
            updatedAtSince = query.updatedAtSince,
            order = sort.orderParams,
        )
        return SearchResult(
            items = response.data.map { it.toSummary() },
            total = response.total,
            offset = response.offset,
            limit = response.limit.takeIf { it > 0 } ?: query.limit,
        )
    }

    override suspend fun getCategories(): List<MangaCategory> =
        api.getMangaTags().data
            .map { it.toCategory() }
            .filter { it.name.isNotBlank() && it.group in SUPPORTED_CATEGORY_GROUPS }
            .sortedWith(compareBy<MangaCategory> { it.group.categoryOrder() }.thenBy { it.name })

    override suspend fun getMangaDetails(mangaId: String): MangaDetails {
        val response = api.getManga(mangaId, MangaDex.MANGA_INCLUDES)
        val dto = response.data ?: throw NoSuchElementException("Manga $mangaId not found")
        return dto.toDetails()
    }

    override suspend fun getChapters(mangaId: String, languages: List<String>): List<Chapter> {
        val all = mutableListOf<Chapter>()
        var offset = 0
        while (true) {
            val response = api.getMangaFeed(
                id = mangaId,
                limit = MangaDex.FEED_PAGE_SIZE,
                offset = offset,
                translatedLanguage = languages,
                contentRating = MangaDex.ALL_CONTENT_RATINGS,
                includes = listOf("scanlation_group"),
                order = mapOf("order[volume]" to "desc", "order[chapter]" to "desc"),
            )
            all += response.data.map { it.toDomain(mangaId) }
            offset += MangaDex.FEED_PAGE_SIZE
            // Stop at the end, on an empty page, or at a safety cap for very
            // long series so we never loop unbounded.
            if (offset >= response.total || response.data.isEmpty() || offset >= 2000) break
        }
        return dedupeChapters(all)
    }

    override suspend fun getPages(chapterId: String, dataSaver: Boolean): List<Page> {
        return api.getAtHomeServer(chapterId).toPages(dataSaver)
    }

    private fun String.categoryOrder(): Int = when (this) {
        "genre" -> 0
        "theme" -> 1
        "content" -> 2
        "format" -> 3
        else -> 3
    }

    private companion object {
        val SUPPORTED_CATEGORY_GROUPS = setOf("genre", "theme", "content", "format")
    }
}
