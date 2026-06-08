package com.mangamojo.app.providers.mangakakalot

import com.mangamojo.app.BuildConfig
import com.mangamojo.app.core.SOURCE_MANGAKAKALOT
import com.mangamojo.app.domain.model.Chapter
import com.mangamojo.app.domain.model.Manga
import com.mangamojo.app.domain.model.MangaCategory
import com.mangamojo.app.domain.model.MangaDetails
import com.mangamojo.app.domain.model.MangaStatus
import com.mangamojo.app.domain.model.Page
import com.mangamojo.app.domain.model.SearchQuery
import com.mangamojo.app.domain.model.SearchResult
import com.mangamojo.app.domain.provider.MangaProvider
import com.mangamojo.app.domain.provider.ProviderItemId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.IOException
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangaKakalotProvider @Inject constructor(
    private val client: OkHttpClient,
) : MangaProvider {

    override val id: String = SOURCE_MANGAKAKALOT
    override val name: String = "MangaKakalot"
    override val baseUrl: String = BuildConfig.MANGAKAKALOT_BASE_URL.trimEnd('/')

    override suspend fun search(query: SearchQuery): SearchResult {
        val title = query.title?.trim().orEmpty()
        if (title.isBlank() || query.includedTagIds.isNotEmpty() || query.updatedAtSince != null) {
            return SearchResult(emptyList(), total = 0, offset = query.offset, limit = query.limit)
        }

        val encodedQuery = URLEncoder.encode(title, Charsets.UTF_8.name()).replace("+", "_")
        val doc = fetchDocument("$baseUrl/search/story/$encodedQuery")
        val allResults = doc.select(SEARCH_RESULT_SELECTORS)
            .mapNotNull { it.toSearchResult() }
            .distinctBy { it.id }
        val pageItems = allResults.drop(query.offset).take(query.limit)

        return SearchResult(
            items = pageItems,
            total = allResults.size,
            offset = query.offset,
            limit = query.limit,
        )
    }

    override suspend fun getCategories(): List<MangaCategory> = emptyList()

    override suspend fun getMangaDetails(mangaId: String): MangaDetails {
        val mangaUrl = ProviderItemId.rawIdForProvider(mangaId, id)
        val doc = fetchDocument(mangaUrl)
        val title = doc.selectFirst("h1, .manga-info-text h1, .story-info-right h1")
            ?.cleanText()
            ?.ifBlank { null }
            ?: "Untitled"
        val coverUrl = doc.selectFirst(".manga-info-pic img, .info-image img, .story-info-left img")
            ?.absoluteImageUrl()
        val description = doc.selectFirst("#noidungm, #panel-story-info-description, .story-info-right-extent")
            ?.cleanText()
            .orEmpty()
        val status = normalizeStatus(doc.infoValue("status"))
        val tags = doc.select("a[href*=genre], .genres a, .story-info-right a[href*=genre]")
            .map { it.cleanText() }
            .filter { it.isNotBlank() }
            .distinct()

        return MangaDetails(
            id = providerMangaId(mangaUrl),
            sourceId = id,
            title = title,
            altTitles = emptyList(),
            description = description,
            coverUrl = coverUrl,
            status = status,
            contentRating = "safe",
            year = null,
            authors = listOfNotBlank(doc.infoValue("author")),
            artists = emptyList(),
            tags = tags,
            availableLanguages = listOf("en"),
        )
    }

    override suspend fun getChapters(mangaId: String, languages: List<String>): List<Chapter> {
        val mangaUrl = ProviderItemId.rawIdForProvider(mangaId, id)
        val stableMangaId = providerMangaId(mangaUrl)
        val doc = fetchDocument(mangaUrl)

        return doc.select(CHAPTER_SELECTORS)
            .mapNotNull { element ->
                val chapterUrl = element.absoluteHref()
                val title = element.cleanText()
                if (chapterUrl.isBlank() || title.isBlank() || !chapterUrl.contains("chapter", ignoreCase = true)) {
                    return@mapNotNull null
                }

                val chapterNumber = extractChapterNumber(title)
                Chapter(
                    id = providerChapterId(chapterUrl),
                    sourceId = id,
                    mangaId = stableMangaId,
                    volume = null,
                    chapter = chapterNumber,
                    title = title,
                    pages = 0,
                    translatedLanguage = "en",
                    scanlationGroup = name,
                    publishAt = null,
                    externalUrl = null,
                    label = chapterLabel(chapterNumber, title),
                )
            }
            .distinctBy { it.id }
    }

    override suspend fun getPages(chapterId: String, dataSaver: Boolean): List<Page> {
        val chapterUrl = ProviderItemId.rawIdForProvider(chapterId, id)
        val doc = fetchDocument(chapterUrl)
        val headers = mapOf(
            "Referer" to chapterUrl,
            "User-Agent" to USER_AGENT,
        )
        val imageElements = PAGE_IMAGE_SELECTOR_PRIORITY
            .asSequence()
            .map { selector -> doc.select(selector) }
            .firstOrNull { it.isNotEmpty() }
            .orEmpty()

        return imageElements
            .mapNotNull { image -> image.absoluteImageUrl() }
            .filter { it.isReadableImageUrl() }
            .distinct()
            .mapIndexed { index, imageUrl -> Page(index = index, imageUrl = imageUrl, headers = headers) }
    }

    override suspend fun isAvailable(): Boolean =
        runCatching { fetchDocument(baseUrl) }.isSuccess

    private suspend fun fetchDocument(url: String): Document = withContext(Dispatchers.IO) {
        val absoluteUrl = absoluteUrl(url)
        val request = Request.Builder()
            .url(absoluteUrl)
            .header("Accept", HTML_ACCEPT)
            .header("Referer", baseUrl)
            .header("User-Agent", USER_AGENT)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("$name request failed: HTTP ${response.code}")
            }
            val body = response.body ?: throw IOException("$name returned an empty response")
            Jsoup.parse(body.string(), absoluteUrl)
        }
    }

    private fun Element.toSearchResult(): Manga? {
        val link = selectFirst(".story_name a[href], h3 a[href], h4 a[href], a[href][title]")
            ?: select("a[href]").firstOrNull { it.attr("title").isNotBlank() || it.cleanText().isNotBlank() }
            ?: return null
        val mangaUrl = link.absoluteHref()
        if (mangaUrl.isBlank()) return null
        val title = link.attr("title").ifBlank { link.cleanText() }
        if (title.isBlank()) return null

        return Manga(
            id = providerMangaId(mangaUrl),
            sourceId = id,
            title = title,
            coverUrl = selectFirst("img")?.absoluteImageUrl(),
            status = MangaStatus.UNKNOWN,
            contentRating = "safe",
            year = null,
        )
    }

    private fun providerMangaId(rawUrl: String): String = ProviderItemId.encode(id, absoluteUrl(rawUrl))

    private fun providerChapterId(rawUrl: String): String = ProviderItemId.encode(id, absoluteUrl(rawUrl))

    private fun Element.absoluteHref(): String = absoluteUrl(absUrl("href").ifBlank { attr("href") })

    private fun Element.absoluteImageUrl(): String? =
        IMAGE_ATTRIBUTES
            .asSequence()
            .map { attr -> absUrl(attr).ifBlank { attr(attr) } }
            .firstOrNull { it.isNotBlank() }
            ?.let { absoluteUrl(it) }

    private fun Document.infoValue(label: String): String? {
        val pattern = Regex(label, RegexOption.IGNORE_CASE)
        return select("li, p, .story-info-right-extent p, .manga-info-text li")
            .firstOrNull { it.cleanText().contains(pattern) }
            ?.cleanText()
            ?.substringAfter(':', missingDelimiterValue = "")
            ?.trim()
            ?.ifBlank { null }
    }

    private fun Element.cleanText(): String = text().replace(WHITESPACE, " ").trim()

    private fun absoluteUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            return trimmed
        }
        return baseUrl.toHttpUrl().resolve(trimmed)?.toString() ?: trimmed
    }

    private fun normalizeStatus(raw: String?): MangaStatus {
        val cleaned = raw.orEmpty().lowercase()
        return when {
            "complete" in cleaned || "finished" in cleaned -> MangaStatus.COMPLETED
            "ongoing" in cleaned || "updating" in cleaned -> MangaStatus.ONGOING
            "hiatus" in cleaned -> MangaStatus.HIATUS
            "cancel" in cleaned -> MangaStatus.CANCELLED
            else -> MangaStatus.UNKNOWN
        }
    }

    private fun extractChapterNumber(title: String): String? =
        CHAPTER_NUMBER.find(title)?.groupValues?.getOrNull(1)

    private fun chapterLabel(chapter: String?, title: String): String =
        if (chapter.isNullOrBlank()) title else "Ch. $chapter - $title"

    private fun listOfNotBlank(value: String?): List<String> =
        value?.takeIf { it.isNotBlank() }?.let { listOf(it) }.orEmpty()

    private fun String.isReadableImageUrl(): Boolean {
        val lowered = substringBefore('?').lowercase()
        return lowered.endsWith(".jpg") ||
            lowered.endsWith(".jpeg") ||
            lowered.endsWith(".png") ||
            lowered.endsWith(".webp")
    }

    private companion object {
        const val HTML_ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
        const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120 Safari/537.36"
        const val SEARCH_RESULT_SELECTORS =
            ".story_item, .itemupdate, .list-truyen-item-wrap, .panel_story_list .story_item"
        const val CHAPTER_SELECTORS =
            ".chapter-list a[href], .row-content-chapter a[href], .panel-story-chapter-list a[href], a[href*=chapter]"
        val PAGE_IMAGE_SELECTOR_PRIORITY = listOf(
            ".container-chapter-reader img",
            ".reading-content img",
            ".vung-doc img",
            "img",
        )
        val IMAGE_ATTRIBUTES = listOf("src", "data-src", "data-original", "data-lazy-src")
        val CHAPTER_NUMBER = Regex("""chapter\s+(\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)
        val WHITESPACE = Regex("\\s+")
    }
}
