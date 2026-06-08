package com.mangamojo.app.providers.mangareader

import com.mangamojo.app.core.SOURCE_MANGAREADER
import com.mangamojo.app.domain.model.MangaStatus
import com.mangamojo.app.domain.model.SearchQuery
import com.mangamojo.app.domain.provider.ProviderItemId
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MangaReaderProviderTest {

    @Test
    fun searchParsesResultsAndDedupesByUrl() = runTest {
        val provider = providerWith(
            "/search" to searchHtml,
        )

        val result = provider.search(SearchQuery(title = "black clover", limit = 10))

        assertEquals(1, result.items.size)
        val item = result.items.first()
        assertEquals(SOURCE_MANGAREADER, item.sourceId)
        assertEquals("Black Clover", item.title)
        assertEquals("https://mangareader.test/black-clover-10", ProviderItemId.rawId(item.id))
        assertEquals("https://mangareader.test/images/black-cover.jpg", item.coverUrl)
    }

    @Test
    fun getMangaDetailsParsesFieldsSafely() = runTest {
        val provider = providerWith(
            "/black-clover-10" to detailsHtml,
        )

        val details = provider.getMangaDetails(
            ProviderItemId.encode(SOURCE_MANGAREADER, "https://mangareader.test/black-clover-10")
        )

        assertEquals("Black Clover", details.title)
        assertEquals("Magic and rivalry.", details.description)
        assertEquals("https://mangareader.test/images/black-cover.jpg", details.coverUrl)
        assertEquals(MangaStatus.ONGOING, details.status)
        assertEquals(2015, details.year)
        assertEquals(listOf("Yuki Tabata"), details.authors)
        assertEquals(listOf("Action", "Fantasy"), details.tags)
    }

    @Test
    fun getChaptersParsesSortsAscendingAndDedupes() = runTest {
        val provider = providerWith(
            "/black-clover-10" to detailsHtml,
        )

        val chapters = provider.getChapters(
            ProviderItemId.encode(SOURCE_MANGAREADER, "https://mangareader.test/black-clover-10"),
            listOf("en"),
        )

        assertEquals(listOf("101", "101.5", "102"), chapters.map { it.chapter })
        assertEquals(3, chapters.distinctBy { it.id }.size)
        assertTrue(chapters.all { it.sourceId == SOURCE_MANGAREADER })
    }

    @Test
    fun getPagesParsesOnlyReaderImagesAndAddsHeaders() = runTest {
        val provider = providerWith(
            "/read/black-clover-10/en/chapter-101.5" to readerHtml,
        )

        val pages = provider.getPages(
            ProviderItemId.encode(
                SOURCE_MANGAREADER,
                "https://mangareader.test/read/black-clover-10/en/chapter-101.5",
            ),
            dataSaver = false,
        )

        assertEquals(
            listOf(
                "https://cdn.mangareader.test/pages/black-101-001.jpg",
                "https://cdn.mangareader.test/pages/black-101-002.webp",
            ),
            pages.map { it.imageUrl },
        )
        assertEquals("https://mangareader.test", pages.first().headers["Referer"])
        assertTrue(pages.first().headers["User-Agent"].orEmpty().contains("Mozilla"))
        assertFalse(pages.any { it.imageUrl.contains("logo") || it.imageUrl.contains("ads") })
    }

    @Test
    fun isAvailableReturnsFalseWhenBaseUrlFails() = runTest {
        val client = HttpClient(MockEngine { respond("", HttpStatusCode.Forbidden) })
        val provider = MangaReaderProvider(client, "https://mangareader.test")

        assertFalse(provider.isAvailable())
    }

    private fun providerWith(vararg responses: Pair<String, String>): MangaReaderProvider {
        val byPath = responses.toMap()
        val engine = MockEngine { request ->
            val content = byPath[request.url.encodedPath].orEmpty()
            respond(
                content = content,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html; charset=utf-8"),
            )
        }
        return MangaReaderProvider(HttpClient(engine), "https://mangareader.test")
    }

    private companion object {
        const val searchHtml = """
            <html><body>
                <div class="manga_list-sbs">
                    <div class="item-spc">
                        <a class="manga-poster" href="/black-clover-10">
                            <img data-src="/images/black-cover.jpg" />
                        </a>
                        <h3 class="manga-name">
                            <a href="/black-clover-10" title="Black Clover">Black Clover</a>
                        </h3>
                    </div>
                    <div class="item-spc">
                        <h3 class="manga-name">
                            <a href="/black-clover-10" title="Black Clover">Black Clover duplicate</a>
                        </h3>
                    </div>
                </div>
            </body></html>
        """

        const val detailsHtml = """
            <html><body>
                <section class="anisc-detail">
                    <h1 class="manga-name">Black Clover</h1>
                    <img class="manga-poster-img" src="/images/black-cover.jpg" />
                    <div class="description">Overview: Magic and rivalry.</div>
                    <div class="anisc-info">
                        <div class="item">Status: Ongoing</div>
                        <div class="item">Released: 2015</div>
                        <div class="item">Author: Yuki Tabata</div>
                        <div class="item item-list">
                            Genres:
                            <a href="/genre/action">Action</a>
                            <a href="/genre/fantasy">Fantasy</a>
                        </div>
                    </div>
                </section>
                <div class="chapter-list">
                    <a href="/read/black-clover-10/en/chapter-102">Chapter 102: Charge</a>
                    <a href="/read/black-clover-10/en/chapter-101.5">Chapter 101.5: Side Story</a>
                    <a href="/read/black-clover-10/en/chapter-101">Chapter 101: Start</a>
                    <a href="/read/black-clover-10/en/chapter-101">Chapter 101: Start duplicate</a>
                </div>
            </body></html>
        """

        const val readerHtml = """
            <html><body>
                <img src="/static/logo.png" />
                <div id="readerarea">
                    <img data-src="https://cdn.mangareader.test/pages/black-101-001.jpg" />
                    <img data-src="https://cdn.mangareader.test/pages/black-101-002.webp" />
                    <img data-src="https://cdn.mangareader.test/ads/banner.jpg" />
                    <img data-src="https://cdn.mangareader.test/avatar/user.png" />
                </div>
            </body></html>
        """
    }
}
