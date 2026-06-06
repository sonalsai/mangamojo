package com.mangamojo.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val mangaId: String,
    val sourceId: String,
    val title: String,
    val coverUrl: String?,
    val status: String,
    val addedAt: Long,
)

/** One row per manga — the most recently read chapter. */
@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey val mangaId: String,
    val sourceId: String,
    val title: String,
    val coverUrl: String?,
    val contentRating: String = "safe",
    val chapterId: String,
    val chapterLabel: String,
    val page: Int,
    val total: Int,
    val readAt: Long,
)

/** Furthest-read page per chapter. */
@Entity(
    tableName = "reading_progress",
    indices = [Index("mangaId")],
)
data class ReadingProgressEntity(
    @PrimaryKey val chapterId: String,
    val mangaId: String,
    val page: Int,
    val total: Int,
    val completed: Boolean,
    val updatedAt: Long,
)

/** User bookmarks. Surfaced minimally in Phase 1; schema is ready for use. */
@Entity(
    tableName = "bookmarks",
    indices = [Index("mangaId"), Index("chapterId")],
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mangaId: String,
    val sourceId: String,
    val chapterId: String,
    val chapterLabel: String,
    val page: Int,
    val note: String?,
    val createdAt: Long,
)
