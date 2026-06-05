package com.mangamojo.app.core

import java.util.concurrent.TimeUnit

/** Source identifier for the only Phase 1 provider. Kept as a constant so the
 *  rest of the app never hard-codes the string and future providers can add
 *  their own. */
const val SOURCE_MANGADEX = "mangadex"

/** MangaDex public API. No key required for read access. */
object MangaDex {
    const val API_BASE = "https://api.mangadex.org/"
    const val UPLOADS_BASE = "https://uploads.mangadex.org"

    const val DEFAULT_LANGUAGE = "en"

    /** Conservative default for a general audience; widened from Settings. */
    val DEFAULT_CONTENT_RATINGS = listOf("safe", "suggestive")
    val ADULT_CONTENT_RATINGS = listOf("erotica", "pornographic")
    val MIXED_CONTENT_RATINGS = DEFAULT_CONTENT_RATINGS + ADULT_CONTENT_RATINGS
    val ALL_CONTENT_RATINGS = listOf("safe", "suggestive", "erotica", "pornographic")

    /** includes[] used on manga requests to inline cover/author/artist. */
    val MANGA_INCLUDES = listOf("cover_art", "author", "artist")

    const val SEARCH_PAGE_SIZE = 24
    const val FEED_PAGE_SIZE = 100

    /** Polite identification per MangaDex API etiquette. */
    const val USER_AGENT = "MangaMojo/1.0 (Android; local-first reader)"
}

/** TTLs and limits that govern how long cached data is treated as fresh and
 *  when the background cleanup worker evicts it. */
object CachePolicy {
    val MANGA_TTL_MS: Long = TimeUnit.HOURS.toMillis(12)
    val CHAPTERS_TTL_MS: Long = TimeUnit.HOURS.toMillis(6)

    /** Cached metadata older than this is eligible for eviction, unless the
     *  manga is a favorite or appears in recent history. */
    val MAX_CACHE_AGE_MS: Long = TimeUnit.DAYS.toMillis(7)

    const val HISTORY_LIMIT = 60
    const val CONTINUE_READING_LIMIT = 12
}
