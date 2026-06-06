package com.mangamojo.app.domain.model

/**
 * One row of reading history — the most recent chapter read for a manga, with
 * enough denormalized data (title/cover/label) to render a card without a
 * network call. Also powers "Continue reading".
 */
data class HistoryEntry(
    val mangaId: String,
    val sourceId: String,
    val title: String,
    val coverUrl: String?,
    val contentRating: String,
    val chapterId: String,
    val chapterLabel: String,
    val page: Int,
    val total: Int,
    val readAt: Long,
) {
    val isCompleted: Boolean get() = total > 0 && page >= total - 1
    val progressFraction: Float
        get() = if (total > 0) (page + 1).toFloat() / total else 0f
}
