package com.mangamojo.app.providers.mangareader

import com.mangamojo.app.BuildConfig
import com.mangamojo.app.core.SOURCE_MANGAREADER
import com.mangamojo.app.domain.model.Chapter
import com.mangamojo.app.domain.model.Manga
import com.mangamojo.app.domain.model.MangaCategory
import com.mangamojo.app.domain.model.MangaDetails
import com.mangamojo.app.domain.model.MangaStatus
import com.mangamojo.app.domain.model.Page
import com.mangamojo.app.domain.model.SearchQuery
import com.mangamojo.app.domain.model.SearchResult
import com.mangamojo.app.domain.provider.MangaProvider
import com.mangamojo.app.domain.provider.ProviderException
import com.mangamojo.app.domain.provider.ProviderItemId
import com.mangamojo.app.providers.common.ProviderHealthChecker
import com.mangamojo.app.providers.common.TimedMemoryCache
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.delay
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URI
import java.net.URLEncoder
import javax.inject.Inject

class MangaReaderProvider(
    private val client: HttpClient,
    override val baseUrl: String,
) : MangaProvider {

    @Inject
    constructor(client: HttpClient) : this(client, BuildConfig.MANGAREADER_BASE_URL.trimEnd('/'))

    override val id: String = SOURCE_MANGAREADER
    override val name: String = "MangaReader"

    private val searchCache = TimedMemoryCache<String, SearchResult>(CACHE_TTL_MS)
    private val detailsCache = TimedMemoryCache<String, MangaDetails>(CACHE_TTL_MS)
    private val chaptersCache = TimedMemoryCache<String, List<Chapter>>(CACHE_TTL_MS)
    private val healthChecker = ProviderHealthChecker(client, USER_AGENT)

    override suspend fun search(query: SearchQuery): SearchResult {
        val title = query.title?.trim().orEmpty()
        if (title.isBlank() || query.includedTagIds.isNotEmpty() || query.updatedAtSince != null) {
            return SearchResult(emptyList(), total = 0, offset = query.offset, limit = query.limit)
        }

        val page = (query.offset / query.limit.coerceAtLeast(1)) + 1
        val cacheKey = listOf(title.lowercase(), page, query.limit).joinToString("|")
        searchCache.get(cacheKey)?.let { return it }

        return runCatching {
            val encoded = URLEncoder.encode(title, Charsets.UTF_8.name())
            val url = "$baseUrl/search?keyword=$encoded&page=$page"
            val doc = Jsoup.parse(fetchHtml(url), url)
            val allResults = doc.select(SEARCH_RESULT_SELECTORS)
                .mapNotNull { it.toSearchResult() }
                .distinctBy { ProviderItemId.rawId(it.id) }
            val pageItems = allResults.take(query.limit)
            SearchResult(
                items = pageItems,
                total = query.offset + pageItems.size + if (pageItems.size >= query.limit) query.limit else 0,
                offset = query.offset,
                limit = query.limit,
            )
        }.getOrElse {
            SearchResult(emptyList(), total = 0, offset = query.offset, limit = query.limit)
        }.also { searchCache.put(cacheKey, it) }
    }

    override suspend fun getCategories(): List<MangaCategory> = emptyList()

    override suspend fun getMangaDetails(mangaId: String): MangaDetails {
        val mangaUrl = ProviderItemId.rawIdForProvider(mangaId, id).absoluteUrl()
        detailsCache.get(mangaUrl)?.let { return it }

        return runCatching {
            val doc = Jsoup.parse(fetchHtml(mangaUrl), mangaUrl)
            val title = doc.selectFirst(TITLE_SELECTORS)?.cleanText()?.ifBlank { null } ?: "Untitled"
            val description = doc.selectFirst(DESCRIPTION_SELECTORS)
                ?.cleanText()
                ?.removePrefix("Overview:")
                ?.trim()
                .orEmpty()
            val tags = doc.select(GENRE_SELECTORS)
                .map { it.cleanText() }
                .filter { it.isNotBlank() }
                .distinct()

            MangaDetails(
                id = providerMangaId(mangaUrl),
                sourceId = id,
                title = title,
                altTitles = emptyList(),
                description = description,
                coverUrl = doc.selectFirst(COVER_SELECTORS)?.absoluteImageUrl(),
                status = normalizeStatus(doc.infoValue("status")),
                contentRating = "safe",
                year = doc.infoValue("released")?.let { YEAR.find(it)?.value?.toIntOrNull() },
                authors = listOfNotBlank(doc.infoValue("author")),
                artists = emptyList(),
                tags = tags,
                availableLanguages = listOf("en"),
            )
        }.getOrElse { cause ->
            throw ProviderException(id, "Unable to load manga details", cause)
        }.also { detailsCache.put(mangaUrl, it) }
    }

    override suspend fun getChapters(mangaId: String, languages: List<String>): List<Chapter> {
        val mangaUrl = ProviderItemId.rawIdForProvider(mangaId, id).absoluteUrl()
        chaptersCache.get(mangaUrl)?.let { return it }

        return runCatching {
            val doc = Jsoup.parse(fetchHtml(mangaUrl), mangaUrl)
            val chapterDocs = buildList {
                add(doc)
                addAll(fetchChapterListDocuments(doc))
            }
            chapterDocs
                .flatMap { it.select(CHAPTER_SELECTORS) }
                .mapIndexedNotNull { index, element -> element.toChapter(mangaUrl, index) }
                .distinctBy { ProviderItemId.rawId(it.chapter.id) }
                .sortedWith(
                    compareBy<IndexedChapter> { it.chapter.chapter.toNumberOrNull() ?: Double.MAX_VALUE }
                        .thenBy { it.index }
                )
                .map { it.chapter }
        }.getOrElse {
            emptyList()
        }.also { chaptersCache.put(mangaUrl, it) }
    }

    override suspend fun getPages(chapterId: String, dataSaver: Boolean): List<Page> {
        val chapterUrl = ProviderItemId.rawIdForProvider(chapterId, id).absoluteUrl()
        return runCatching {
            val doc = Jsoup.parse(fetchHtml(chapterUrl), chapterUrl)
            val htmlImageUrls = PAGE_IMAGE_SELECTOR_PRIORITY
                .asSequence()
                .map { selector -> doc.select(selector) }
                .firstOrNull { it.isNotEmpty() }
                .orEmpty()
                .mapNotNull { it.absoluteImageUrl() }
            val scriptImageUrls = doc.select("script")
                .flatMap { script -> IMAGE_URL.findAll(script.data()).map { it.value.unescapeJsonUrl() }.toList() }

            (htmlImageUrls + scriptImageUrls)
                .map { it.absoluteUrl(chapterUrl) }
                .filter { it.isMangaPageImage() }
                .distinct()
                .mapIndexed { index, imageUrl ->
                    Page(
                        index = index,
                        imageUrl = imageUrl,
                        headers = mapOf(
                            "Referer" to baseUrl,
                            "User-Agent" to USER_AGENT,
                        ),
                    )
                }
        }.getOrElse {
            emptyList()
        }
    }

    override suspend fun isAvailable(): Boolean = healthChecker.isAvailable(baseUrl)

    private suspend fun fetchHtml(url: String): String {
        val target = url.absoluteUrl()
        var nextDelayMs = INITIAL_BACKOFF_MS
        var lastFailure: Throwable? = null

        repeat(MAX_ATTEMPTS) { attempt ->
            val result = runCatching {
                val response = client.get(target) {
                    header(HttpHeaders.UserAgent, USER_AGENT)
                    header(HttpHeaders.Referrer, baseUrl)
                    header(HttpHeaders.AcceptLanguage, ACCEPT_LANGUAGE)
                    header(HttpHeaders.Accept, HTML_ACCEPT)
                }
                if (!response.status.isSuccess()) {
                    throw ProviderException(id, "HTTP ${response.status.value} for $target")
                }
                response.bodyAsText()
            }
            result.getOrNull()?.let { return it }
            lastFailure = result.exceptionOrNull()
            if (attempt < MAX_ATTEMPTS - 1) {
                delay(nextDelayMs)
                nextDelayMs *= 2
            }
        }

        throw ProviderException(id, "Failed to fetch $target after $MAX_ATTEMPTS attempts", lastFailure)
    }

    private suspend fun fetchChapterListDocuments(doc: Document): List<Document> {
        val mangaDataId = doc.selectFirst("[data-id], [data-manga-id], input#manga-id, input[name=manga_id]")
            ?.let { element ->
                element.attr("data-id")
                    .ifBlank { element.attr("data-manga-id") }
                    .ifBlank { element.attr("value") }
            }
            ?.takeIf { it.isNotBlank() }
            ?: return emptyList()

        val ajaxUrls = listOf(
            "$baseUrl/ajax/manga/list-chapter?mangaId=$mangaDataId",
            "$baseUrl/ajax/manga/chapter/list/$mangaDataId",
            "$baseUrl/ajax/chapter/list/$mangaDataId",
        )

        return ajaxUrls.mapNotNull { url ->
            runCatching {
                val payload = fetchHtml(url).extractHtmlPayload()
                Jsoup.parse(payload, baseUrl)
            }.getOrNull()
        }
    }

    private fun Element.toSearchResult(): Manga? {
        val link = selectFirst(RESULT_LINK_SELECTORS)
            ?: select("a[href]").firstOrNull { it.cleanText().isNotBlank() || it.attr("title").isNotBlank() }
            ?: return null
        val mangaUrl = link.absoluteHref()
        if (mangaUrl.isBlank() || mangaUrl.contains("/read/", ignoreCase = true)) return null
        val title = link.attr("title").ifBlank { link.cleanText() }.ifBlank { return null }

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

    private fun Element.toChapter(mangaUrl: String, index: Int): IndexedChapter? {
        val chapterUrl = absoluteHref()
        val text = cleanText()
        if (chapterUrl.isBlank() || !chapterUrl.contains("chapter", ignoreCase = true)) return null
        val chapterNumber = extractChapterNumber(text).ifBlank { extractChapterNumber(chapterUrl) }
        val title = text.ifBlank { chapterNumber.takeIf { it.isNotBlank() }?.let { "Chapter $it" }.orEmpty() }
        if (title.isBlank()) return null

        return IndexedChapter(
            index = index,
            chapter = Chapter(
                id = providerChapterId(chapterUrl),
                sourceId = id,
                mangaId = providerMangaId(mangaUrl),
                volume = null,
                chapter = chapterNumber.ifBlank { null },
                title = title,
                pages = 0,
                translatedLanguage = "en",
                scanlationGroup = name,
                publishAt = null,
                externalUrl = null,
                label = chapterLabel(chapterNumber, title),
            ),
        )
    }

    private fun providerMangaId(rawUrl: String): String = ProviderItemId.encode(id, rawUrl.absoluteUrl())

    private fun providerChapterId(rawUrl: String): String = ProviderItemId.encode(id, rawUrl.absoluteUrl())

    private fun Element.absoluteHref(): String =
        absUrl("href").ifBlank { attr("href") }.absoluteUrl(baseUrl)

    private fun Element.absoluteImageUrl(): String? =
        IMAGE_ATTRIBUTES
            .asSequence()
            .map { attr -> absUrl(attr).ifBlank { attr(attr) } }
            .firstOrNull { it.isNotBlank() }
            ?.unescapeJsonUrl()
            ?.absoluteUrl(baseUrl)

    private fun Document.infoValue(label: String): String? {
        val labelRegex = Regex(label, RegexOption.IGNORE_CASE)
        return select(".anisc-info .item, .manga-info .item, .item, li, p")
            .firstOrNull { it.cleanText().contains(labelRegex) }
            ?.cleanText()
            ?.substringAfter(':', missingDelimiterValue = "")
            ?.trim()
            ?.ifBlank { null }
    }

    private fun Element.cleanText(): String = text().replace(WHITESPACE, " ").trim()

    private fun String.extractHtmlPayload(): String {
        val trimmed = trim()
        if (!trimmed.startsWith("{")) return this
        val encodedHtml = JSON_HTML_FIELD.find(trimmed)?.groupValues?.getOrNull(1) ?: return this
        return encodedHtml
            .replace("\\/", "/")
            .replace("\\\"", "\"")
            .replace("\\n", "")
            .replace("\\t", "")
    }

    private fun String.absoluteUrl(base: String = baseUrl): String {
        val trimmed = trim()
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            return trimmed
        }
        return URI(base.trimEnd('/') + "/").resolve(trimmed).toString()
    }

    private fun String.unescapeJsonUrl(): String =
        replace("\\/", "/").replace("\\u0026", "&")

    private fun normalizeStatus(raw: String?): MangaStatus {
        val cleaned = raw.orEmpty().lowercase()
        return when {
            "complete" in cleaned || "finished" in cleaned -> MangaStatus.COMPLETED
            "ongoing" in cleaned || "publishing" in cleaned -> MangaStatus.ONGOING
            "hiatus" in cleaned -> MangaStatus.HIATUS
            "cancel" in cleaned -> MangaStatus.CANCELLED
            else -> MangaStatus.UNKNOWN
        }
    }

    private fun extractChapterNumber(value: String): String =
        CHAPTER_NUMBER.find(value)?.groupValues?.getOrNull(1).orEmpty()

    private fun chapterLabel(chapter: String, title: String): String =
        if (chapter.isBlank()) title else "Ch. $chapter - $title"

    private fun listOfNotBlank(value: String?): List<String> =
        value?.takeIf { it.isNotBlank() }?.let { listOf(it) }.orEmpty()

    private fun String.isMangaPageImage(): Boolean {
        val lowered = substringBefore('?').lowercase()
        if (!IMAGE_EXTENSIONS.any { lowered.endsWith(it) }) return false
        return BLOCKED_IMAGE_TERMS.none { term -> lowered.contains(term) }
    }

    private fun String?.toNumberOrNull(): Double? = this?.trim()?.toDoubleOrNull()

    private data class IndexedChapter(
        val index: Int,
        val chapter: Chapter,
    )

    private companion object {
        const val CACHE_TTL_MS = 5 * 60 * 1000L
        const val MAX_ATTEMPTS = 3
        const val INITIAL_BACKOFF_MS = 250L
        const val HTML_ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
        const val ACCEPT_LANGUAGE = "en-US,en;q=0.9"
        const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120 Safari/537.36"

        const val SEARCH_RESULT_SELECTORS =
            ".manga_list-sbs .item-spc, .manga_list-sbs .item, .film_list-wrap .flw-item, .item-spc, .manga-item"
        const val RESULT_LINK_SELECTORS =
            ".manga-name a[href], .dynamic-name[href], .film-name a[href], h3 a[href], h4 a[href], a[href*='/manga/']"
        const val TITLE_SELECTORS = ".manga-name, .anisc-detail .manga-name, h1, h2"
        const val DESCRIPTION_SELECTORS =
            ".description, .manga-description, .anisc-description, .detail-desc, #description"
        const val COVER_SELECTORS = ".manga-poster-img, .film-poster-img, .poster img, img"
        const val GENRE_SELECTORS = ".item-list a[href*='/genre/'], a[href*='/genre/']"
        const val CHAPTER_SELECTORS =
            ".chapter-list a[href], .chapters-list a[href], .chapter-item a[href], .item a[href*='chapter'], a[href*='/read/'][href*='chapter']"
        val PAGE_IMAGE_SELECTOR_PRIORITY = listOf(
            "#readerarea img",
            ".reading-content img",
            ".container-reader-chapter img",
            ".chapter-reader img",
            ".iv-card img",
            ".page-reader img",
            "img.chapter-img",
            "img[data-src]",
            "img[src]",
        )
        val IMAGE_ATTRIBUTES = listOf("data-src", "data-original", "data-url", "src")
        val IMAGE_EXTENSIONS = listOf(".jpg", ".jpeg", ".png", ".webp")
        val BLOCKED_IMAGE_TERMS = listOf(
            "logo",
            "avatar",
            "banner",
            "ads",
            "advert",
            "cover",
            "poster",
            "icon",
            "placeholder",
        )
        val CHAPTER_NUMBER = Regex("""(?:chapter|chap|ch\.?)\s*(\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)
        val IMAGE_URL = Regex("""https?:\\?/\\?/[^"'\\\s]+?\.(?:jpg|jpeg|png|webp)(?:\?[^"'\\\s]*)?""", RegexOption.IGNORE_CASE)
        val JSON_HTML_FIELD = Regex(""""html"\s*:\s*"(.*?)"""")
        val YEAR = Regex("""\b(19|20)\d{2}\b""")
        val WHITESPACE = Regex("\\s+")
    }
}
