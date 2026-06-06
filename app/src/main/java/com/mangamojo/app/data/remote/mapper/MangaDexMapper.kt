package com.mangamojo.app.data.remote.mapper

import com.mangamojo.app.core.MangaDex
import com.mangamojo.app.core.SOURCE_MANGADEX
import com.mangamojo.app.data.remote.dto.AtHomeResponseDto
import com.mangamojo.app.data.remote.dto.ChapterDto
import com.mangamojo.app.data.remote.dto.MangaDto
import com.mangamojo.app.data.remote.dto.TagDto
import com.mangamojo.app.domain.model.Chapter
import com.mangamojo.app.domain.model.Manga
import com.mangamojo.app.domain.model.MangaCategory
import com.mangamojo.app.domain.model.MangaDetails
import com.mangamojo.app.domain.model.MangaStatus
import com.mangamojo.app.domain.model.Page

/**
 * Translates raw MangaDex DTOs into the app's source-agnostic domain models.
 * Logic mirrors the reference POC's `lib/manga.js` so behavior is consistent
 * across the web and Android clients. Exposed as top-level extensions so the
 * provider can write `dto.toSummary()` directly.
 */

/** Pick a value from a localized map, preferring the requested language. */
internal fun pickLocalized(
    map: Map<String, String>,
    lang: String = MangaDex.DEFAULT_LANGUAGE,
): String {
    if (map.isEmpty()) return ""
    map[lang]?.let { return it }
    map["en"]?.let { return it }
    return map.values.firstOrNull().orEmpty()
}

/** Build a cover URL. size = null (full) | 256 | 512. */
internal fun coverUrl(mangaId: String, fileName: String?, size: Int? = 512): String? {
    if (fileName.isNullOrBlank()) return null
    val suffix = if (size != null) ".$size.jpg" else ""
    return "${MangaDex.UPLOADS_BASE}/covers/$mangaId/$fileName$suffix"
}

private fun MangaDto.coverFileName(): String? =
    relationships.firstOrNull { it.type == "cover_art" }?.attributes?.fileName

fun MangaDto.toSummary(): Manga {
    val altFirst = attributes.altTitles.firstOrNull()?.let { pickLocalized(it) }
    return Manga(
        id = id,
        sourceId = SOURCE_MANGADEX,
        title = pickLocalized(attributes.title).ifBlank { altFirst ?: "Untitled" },
        coverUrl = coverUrl(id, coverFileName(), 512),
        status = MangaStatus.from(attributes.status),
        contentRating = attributes.contentRating ?: "safe",
        year = attributes.year,
    )
}

fun MangaDto.toDetails(): MangaDetails {
    val authors = relationships.filter { it.type == "author" }
        .mapNotNull { it.attributes?.name }.distinct()
    val artists = relationships.filter { it.type == "artist" }
        .mapNotNull { it.attributes?.name }.distinct()
    val altTitles = attributes.altTitles.map { pickLocalized(it) }.filter { it.isNotBlank() }
    val tags = attributes.tags.map { pickLocalized(it.attributes.name) }.filter { it.isNotBlank() }
    return MangaDetails(
        id = id,
        sourceId = SOURCE_MANGADEX,
        title = pickLocalized(attributes.title).ifBlank { altTitles.firstOrNull() ?: "Untitled" },
        altTitles = altTitles,
        description = pickLocalized(attributes.description),
        coverUrl = coverUrl(id, coverFileName(), 512),
        status = MangaStatus.from(attributes.status),
        contentRating = attributes.contentRating ?: "safe",
        year = attributes.year,
        authors = authors,
        artists = artists,
        tags = tags,
        availableLanguages = attributes.availableTranslatedLanguages.filterNotNull(),
    )
}

/** Readable label like "Vol. 2 Ch. 14 — The Promise". */
fun TagDto.toCategory(): MangaCategory = MangaCategory(
    id = id,
    name = pickLocalized(attributes.name).ifBlank { "Category" },
    group = attributes.group.orEmpty(),
)

private fun chapterLabel(volume: String?, chapter: String?, title: String?): String {
    val parts = buildList {
        if (!volume.isNullOrBlank()) add("Vol. $volume")
        if (!chapter.isNullOrBlank()) add("Ch. $chapter")
        else if (volume.isNullOrBlank()) add("Oneshot")
    }
    var label = parts.joinToString(" ")
    if (!title.isNullOrBlank()) label += " — $title"
    return label.ifBlank { "Chapter" }
}

fun ChapterDto.toDomain(mangaId: String): Chapter {
    val group = relationships.firstOrNull { it.type == "scanlation_group" }
        ?.attributes?.name ?: "No Group"
    val a = attributes
    return Chapter(
        id = id,
        sourceId = SOURCE_MANGADEX,
        mangaId = mangaId,
        volume = a.volume,
        chapter = a.chapter,
        title = a.title.orEmpty(),
        pages = a.pages,
        translatedLanguage = a.translatedLanguage,
        scanlationGroup = group,
        publishAt = a.publishAt,
        externalUrl = a.externalUrl,
        label = chapterLabel(a.volume, a.chapter, a.title),
    )
}

/**
 * MangaDex returns multiple group uploads of the same chapter number. Collapse
 * to one entry per (volume, chapter), preferring an in-app readable upload over
 * an external one. Input order is preserved via [LinkedHashMap].
 */
fun dedupeChapters(chapters: List<Chapter>): List<Chapter> {
    val seen = LinkedHashMap<String, Chapter>()
    for (ch in chapters) {
        val key = if (ch.chapter != null) {
            "${ch.volume ?: ""}::ch::${ch.chapter}"
        } else {
            "${ch.volume ?: ""}::oneshot::${ch.title.ifBlank { ch.id }}"
        }
        val existing = seen[key]
        when {
            existing == null -> seen[key] = ch
            existing.isExternal && !ch.isExternal -> seen[key] = ch
        }
    }
    return seen.values.toList()
}

/** Resolve MangaDex@Home filenames into absolute page image URLs. */
fun AtHomeResponseDto.toPages(dataSaver: Boolean): List<Page> {
    val mode = if (dataSaver) "data-saver" else "data"
    val files = if (dataSaver) chapter.dataSaver else chapter.data
    return files.mapIndexed { index, file ->
        Page(index = index, imageUrl = "$baseUrl/$mode/${chapter.hash}/$file")
    }
}
