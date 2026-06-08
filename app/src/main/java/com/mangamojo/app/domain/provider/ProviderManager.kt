package com.mangamojo.app.domain.provider

import com.mangamojo.app.core.SOURCE_MANGADEX
import com.mangamojo.app.core.SOURCE_MANGAKAKALOT
import com.mangamojo.app.core.SOURCE_MANGAREADER
import com.mangamojo.app.domain.model.Chapter
import com.mangamojo.app.domain.model.Manga
import com.mangamojo.app.domain.model.MangaCategory
import com.mangamojo.app.domain.model.MangaDetails
import com.mangamojo.app.domain.model.SearchQuery
import com.mangamojo.app.domain.model.SearchResult
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProviderManager @Inject constructor(
    providers: Set<@JvmSuppressWildcards MangaProvider>,
) {
    private val orderedProviders: List<MangaProvider> =
        providers.sortedWith(compareBy<MangaProvider> { providerPriority(it.id) }.thenBy { it.name })
    private val providersById: Map<String, MangaProvider> = orderedProviders.associateBy { it.id }

    val availableProviders: List<MangaProvider> get() = orderedProviders

    suspend fun getHealthyProviders(): List<MangaProvider> = coroutineScope {
        orderedProviders
            .map { provider -> async { provider to runCatching { provider.isAvailable() }.getOrDefault(false) } }
            .awaitAll()
            .filter { (_, healthy) -> healthy }
            .map { (provider, _) -> provider }
    }

    fun providerForResource(resourceId: String): MangaProvider {
        val sourceId = ProviderItemId.sourceId(resourceId)
        if (sourceId != null) {
            return providersById[sourceId]
                ?: throw NoSuchElementException("Provider $sourceId is not available")
        }
        return providersById[SOURCE_MANGADEX]
            ?: orderedProviders.firstOrNull()
            ?: throw NoSuchElementException("No manga providers are available")
    }

    suspend fun search(query: SearchQuery): SearchResult = coroutineScope {
        val selectedProviders = query.sourceId
            ?.let { id -> listOfNotNull(providersById[id]) }
            ?: orderedProviders
        if (selectedProviders.isEmpty()) {
            throw NoSuchElementException("Provider ${query.sourceId} is not available")
        }

        val attempts = selectedProviders
            .map { provider -> async { runCatching { provider.search(query) } } }
            .awaitAll()

        val successful = attempts.mapNotNull { it.getOrNull() }
        if (successful.isEmpty()) {
            attempts.firstOrNull()?.exceptionOrNull()?.let { throw it }
            return@coroutineScope SearchResult(emptyList(), total = 0, offset = query.offset, limit = query.limit)
        }

        val merged = successful
            .flatMap { it.items }
            .distinctBy { it.dedupeKey() }
        val pageItems = merged.take(query.limit)
        val hasMore = merged.size > pageItems.size || successful.any { it.hasMore }
        SearchResult(
            items = pageItems,
            total = query.offset + pageItems.size + if (hasMore) query.limit else 0,
            offset = query.offset,
            limit = query.limit,
        )
    }

    suspend fun getCategories(): List<MangaCategory> {
        var firstFailure: Throwable? = null
        for (provider in orderedProviders) {
            val categories = runCatching { provider.getCategories() }
                .onFailure { if (firstFailure == null) firstFailure = it }
                .getOrNull()
                .orEmpty()
            if (categories.isNotEmpty()) return categories
        }
        firstFailure?.let { throw it }
        return emptyList()
    }

    suspend fun getMergedChapters(
        mangaId: String,
        details: MangaDetails,
        languages: List<String>,
    ): List<Chapter> = coroutineScope {
        val primaryProvider = providerForResource(mangaId)
        val primaryAttempt = async { runCatching { primaryProvider.getChapters(mangaId, languages) } }
        val supplementalAttempts = orderedProviders
            .filterNot { it.id == primaryProvider.id }
            .map { provider ->
                async {
                    runCatching {
                        val match = provider.findMatchingManga(details) ?: return@runCatching emptyList()
                        provider.getChapters(match.id, languages)
                            .map { chapter -> chapter.copy(mangaId = mangaId) }
                    }
                }
            }

        val primaryResult = primaryAttempt.await()
        val primaryChapters = primaryResult.getOrDefault(emptyList())
        val supplementalResults = supplementalAttempts.awaitAll()
        val supplementalChapters = supplementalResults
            .mapNotNull { it.getOrNull() }
            .flatten()

        val merged = mergeChapters(primaryChapters, supplementalChapters)
        if (merged.isNotEmpty()) return@coroutineScope merged

        primaryResult.exceptionOrNull()?.let { throw it }
        supplementalResults
            .mapNotNull { it.exceptionOrNull() }
            .firstOrNull()
            ?.let { throw it }
        emptyList()
    }

    private suspend fun MangaProvider.findMatchingManga(details: MangaDetails): Manga? {
        val targetTitles = details.matchTitles()
        for (title in targetTitles) {
            val result = runCatching {
                search(SearchQuery(title = title, limit = SUPPLEMENT_SEARCH_LIMIT, sourceId = id))
            }.getOrNull() ?: continue
            result.items.firstOrNull { it.title.normalizedTitle() in targetTitles }?.let { return it }
        }
        return null
    }

    private fun MangaDetails.matchTitles(): Set<String> =
        (listOf(title) + altTitles)
            .map { it.normalizedTitle() }
            .filter { it.isNotBlank() }
            .toSet()

    private fun mergeChapters(
        primaryChapters: List<Chapter>,
        supplementalChapters: List<Chapter>,
    ): List<Chapter> {
        val merged = LinkedHashMap<String, Chapter>()
        (primaryChapters + supplementalChapters).forEach { chapter ->
            merged.putIfAbsent(chapter.dedupeKey(), chapter)
        }
        return merged.values
            .mapIndexed { index, chapter -> IndexedChapter(index, chapter) }
            .sortedWith(
                compareByDescending<IndexedChapter> { it.chapter.volume.toNumberOrNull() ?: -1.0 }
                    .thenByDescending { it.chapter.chapter.toNumberOrNull() ?: -1.0 }
                    .thenBy { it.index }
            )
            .map { it.chapter }
    }

    private fun Chapter.dedupeKey(): String {
        val chapterNumber = chapter?.trim()
        if (!chapterNumber.isNullOrBlank()) {
            return "${volume.orEmpty()}::chapter::${chapterNumber.lowercase()}"
        }
        return "${volume.orEmpty()}::title::${label.ifBlank { title }.normalizedTitle()}"
    }

    private fun Manga.dedupeKey(): String {
        return "${title.normalizedTitle()}::${year ?: "unknown"}"
    }

    private fun String.normalizedTitle(): String =
        lowercase()
            .map { if (it.isLetterOrDigit()) it else ' ' }
            .joinToString("")
            .trim()
            .replace(WHITESPACE, " ")

    private fun String?.toNumberOrNull(): Double? =
        this?.trim()?.toDoubleOrNull()

    private data class IndexedChapter(
        val index: Int,
        val chapter: Chapter,
    )

    private fun providerPriority(providerId: String): Int = when (providerId) {
        SOURCE_MANGADEX -> 0
        SOURCE_MANGAREADER -> 10
        SOURCE_MANGAKAKALOT -> 20
        else -> 10
    }

    private companion object {
        const val SUPPLEMENT_SEARCH_LIMIT = 5
        val WHITESPACE = Regex("\\s+")
    }
}
