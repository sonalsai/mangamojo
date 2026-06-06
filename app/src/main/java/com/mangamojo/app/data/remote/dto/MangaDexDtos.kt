package com.mangamojo.app.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonTransformingSerializer

/**
 * MangaDex occasionally serializes an empty localized object (`{}`) as an empty
 * array (`[]`) — most often for `description`. This transforming serializer
 * coerces such arrays back into an empty map so decoding never blows up.
 */
object LenientStringMapSerializer : JsonTransformingSerializer<Map<String, String>>(
    MapSerializer(String.serializer(), String.serializer())
) {
    override fun transformDeserialize(element: JsonElement): JsonElement =
        if (element is JsonArray) JsonObject(emptyMap()) else element
}

/* ------------------------------------------------------------------ *
 * Shared
 * ------------------------------------------------------------------ */

@Serializable
data class RelationshipDto(
    val id: String,
    val type: String,
    val attributes: RelationshipAttributesDto? = null,
)

/** Attributes inlined via `includes[]`; fields vary by relationship type. */
@Serializable
data class RelationshipAttributesDto(
    val fileName: String? = null, // cover_art
    val name: String? = null,     // author / artist / scanlation_group
)

/* ------------------------------------------------------------------ *
 * Manga
 * ------------------------------------------------------------------ */

@Serializable
data class MangaListResponseDto(
    val result: String? = null,
    val data: List<MangaDto> = emptyList(),
    val limit: Int = 0,
    val offset: Int = 0,
    val total: Int = 0,
)

@Serializable
data class MangaResponseDto(
    val result: String? = null,
    val data: MangaDto? = null,
)

@Serializable
data class TagListResponseDto(
    val result: String? = null,
    val data: List<TagDto> = emptyList(),
)

@Serializable
data class MangaDto(
    val id: String,
    val type: String? = null,
    val attributes: MangaAttributesDto = MangaAttributesDto(),
    val relationships: List<RelationshipDto> = emptyList(),
)

@Serializable
data class MangaAttributesDto(
    @Serializable(with = LenientStringMapSerializer::class)
    val title: Map<String, String> = emptyMap(),
    val altTitles: List<Map<String, String>> = emptyList(),
    @Serializable(with = LenientStringMapSerializer::class)
    val description: Map<String, String> = emptyMap(),
    val status: String? = null,
    val year: Int? = null,
    val contentRating: String? = null,
    val publicationDemographic: String? = null,
    val originalLanguage: String? = null,
    val lastChapter: String? = null,
    val lastVolume: String? = null,
    val availableTranslatedLanguages: List<String?> = emptyList(),
    val tags: List<TagDto> = emptyList(),
    val updatedAt: String? = null,
    val createdAt: String? = null,
)

@Serializable
data class TagDto(
    val id: String,
    val attributes: TagAttributesDto = TagAttributesDto(),
)

@Serializable
data class TagAttributesDto(
    @Serializable(with = LenientStringMapSerializer::class)
    val name: Map<String, String> = emptyMap(),
    val group: String? = null,
)

/* ------------------------------------------------------------------ *
 * Chapter feed
 * ------------------------------------------------------------------ */

@Serializable
data class ChapterListResponseDto(
    val result: String? = null,
    val data: List<ChapterDto> = emptyList(),
    val limit: Int = 0,
    val offset: Int = 0,
    val total: Int = 0,
)

@Serializable
data class ChapterDto(
    val id: String,
    val attributes: ChapterAttributesDto = ChapterAttributesDto(),
    val relationships: List<RelationshipDto> = emptyList(),
)

@Serializable
data class ChapterAttributesDto(
    val volume: String? = null,
    val chapter: String? = null,
    val title: String? = null,
    val translatedLanguage: String? = null,
    val externalUrl: String? = null,
    val pages: Int = 0,
    val publishAt: String? = null,
    val readableAt: String? = null,
)

/* ------------------------------------------------------------------ *
 * MangaDex@Home (page delivery)
 * ------------------------------------------------------------------ */

@Serializable
data class AtHomeResponseDto(
    val result: String? = null,
    val baseUrl: String = "",
    val chapter: AtHomeChapterDto = AtHomeChapterDto(),
)

@Serializable
data class AtHomeChapterDto(
    val hash: String = "",
    val data: List<String> = emptyList(),
    val dataSaver: List<String> = emptyList(),
)
