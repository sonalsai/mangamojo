package com.mangamojo.app.domain.model

import com.mangamojo.app.core.MangaDex

/** Sort orders, each carrying the provider-neutral intent + MangaDex mapping. */
enum class SearchSort(val orderParams: Map<String, String>) {
    RELEVANCE(mapOf("order[relevance]" to "desc")),
    POPULAR(mapOf("order[followedCount]" to "desc")),
    LATEST(mapOf("order[latestUploadedChapter]" to "desc")),
    NEWEST(mapOf("order[createdAt]" to "desc")),
    RATING(mapOf("order[rating]" to "desc")),
}

/**
 * Source-agnostic search/browse parameters. The provider layer translates this
 * into whatever the underlying API expects.
 */
data class SearchQuery(
    val title: String? = null,
    val offset: Int = 0,
    val limit: Int = MangaDex.SEARCH_PAGE_SIZE,
    val sort: SearchSort = SearchSort.RELEVANCE,
    val contentRatings: List<String> = MangaDex.DEFAULT_CONTENT_RATINGS,
    val includedTagIds: List<String> = emptyList(),
    val updatedAtSince: String? = null,
)
