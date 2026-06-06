package com.mangamojo.app.data.local.mapper

import com.mangamojo.app.data.local.entity.CachedChapterEntity
import com.mangamojo.app.data.local.entity.CachedMangaEntity
import com.mangamojo.app.data.local.entity.FavoriteEntity
import com.mangamojo.app.data.local.entity.HistoryEntity
import com.mangamojo.app.data.local.entity.ReadingProgressEntity
import com.mangamojo.app.domain.model.Chapter
import com.mangamojo.app.domain.model.Favorite
import com.mangamojo.app.domain.model.HistoryEntry
import com.mangamojo.app.domain.model.Manga
import com.mangamojo.app.domain.model.MangaDetails
import com.mangamojo.app.domain.model.MangaStatus
import com.mangamojo.app.domain.model.ReadingProgress

/** Conversions between Room entities and domain models. */

fun MangaDetails.toCacheEntity(now: Long): CachedMangaEntity = CachedMangaEntity(
    mangaId = id,
    sourceId = sourceId,
    title = title,
    coverUrl = coverUrl,
    status = status.raw,
    contentRating = contentRating,
    year = year,
    description = description,
    altTitles = altTitles,
    authors = authors,
    artists = artists,
    tags = tags,
    availableLanguages = availableLanguages,
    cachedAt = now,
)

fun CachedMangaEntity.toDetails(): MangaDetails = MangaDetails(
    id = mangaId,
    sourceId = sourceId,
    title = title,
    altTitles = altTitles,
    description = description,
    coverUrl = coverUrl,
    status = MangaStatus.from(status),
    contentRating = contentRating,
    year = year,
    authors = authors,
    artists = artists,
    tags = tags,
    availableLanguages = availableLanguages,
)

fun CachedMangaEntity.toSummary(): Manga = Manga(
    id = mangaId,
    sourceId = sourceId,
    title = title,
    coverUrl = coverUrl,
    status = MangaStatus.from(status),
    contentRating = contentRating,
    year = year,
)

fun Chapter.toCacheEntity(orderIndex: Int, now: Long): CachedChapterEntity = CachedChapterEntity(
    chapterId = id,
    mangaId = mangaId,
    sourceId = sourceId,
    volume = volume,
    chapter = chapter,
    title = title,
    pages = pages,
    translatedLanguage = translatedLanguage,
    scanlationGroup = scanlationGroup,
    publishAt = publishAt,
    externalUrl = externalUrl,
    label = label,
    orderIndex = orderIndex,
    cachedAt = now,
)

fun CachedChapterEntity.toDomain(): Chapter = Chapter(
    id = chapterId,
    sourceId = sourceId,
    mangaId = mangaId,
    volume = volume,
    chapter = chapter,
    title = title,
    pages = pages,
    translatedLanguage = translatedLanguage,
    scanlationGroup = scanlationGroup,
    publishAt = publishAt,
    externalUrl = externalUrl,
    label = label,
)

fun FavoriteEntity.toDomain(): Favorite = Favorite(
    mangaId = mangaId,
    sourceId = sourceId,
    title = title,
    coverUrl = coverUrl,
    status = MangaStatus.from(status),
    addedAt = addedAt,
)

fun HistoryEntity.toDomain(): HistoryEntry = HistoryEntry(
    mangaId = mangaId,
    sourceId = sourceId,
    title = title,
    coverUrl = coverUrl,
    contentRating = contentRating,
    chapterId = chapterId,
    chapterLabel = chapterLabel,
    page = page,
    total = total,
    readAt = readAt,
)

fun ReadingProgressEntity.toDomain(): ReadingProgress = ReadingProgress(
    mangaId = mangaId,
    chapterId = chapterId,
    page = page,
    total = total,
    completed = completed,
    updatedAt = updatedAt,
)
