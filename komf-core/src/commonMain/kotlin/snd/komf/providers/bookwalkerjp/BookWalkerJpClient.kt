package snd.komf.providers.bookwalkerjp

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.cookie
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import snd.komf.model.Image
import snd.komf.providers.bookwalkerjp.model.BookWalkerJpSearchResult
import snd.komf.providers.bookwalkerjp.model.BookWalkerJpSeries
import snd.komf.providers.bookwalkerjp.model.BookWalkerJpSeriesId

const val bookWalkerJpBaseUrl = "https://bookwalker.jp"

/**
 * The Japanese BOOK☆WALKER store.
 *
 * Distinct from the existing BookWalker provider, which talks to global.bookwalker.jp -
 * the English storefront, whose catalogue is a small licensed subset. Anything Japanese
 * and not translated is absent there and present here.
 */
class BookWalkerJpClient(
    private val ktor: HttpClient,
) {
    private val parser = BookWalkerJpParser()

    suspend fun searchSeries(name: String): List<BookWalkerJpSearchResult> {
        val html = ktor.get("$bookWalkerJpBaseUrl/search/") {
            parameter("word", name)
            browserHeaders()
        }.bodyAsText()
        return parser.parseSearchResults(html)
    }

    /** The store's own category, e.g. マンガ（漫画） or ライトノベル. */
    suspend fun getCategory(id: BookWalkerJpSeriesId): String? = parser.parseSeriesPage(seriesHtml(id)).first

    suspend fun getSeries(
        id: BookWalkerJpSeriesId,
        fallbackTitle: String,
    ): BookWalkerJpSeries {
        val seriesHtml = seriesHtml(id)
        val firstBook = parser.parseSeriesPage(seriesHtml).second.firstOrNull()
        val bookHtml = firstBook?.let { ktor.get(it) { browserHeaders() }.bodyAsText() }
        return parser.parseSeries(id, seriesHtml, bookHtml, fallbackTitle)
    }

    suspend fun getThumbnail(url: String?): Image? {
        if (url == null) return null
        val bytes: ByteArray = ktor.get(url) { browserHeaders() }.body()
        return Image(bytes)
    }

    private suspend fun seriesHtml(id: BookWalkerJpSeriesId) = ktor.get(id.url) { browserHeaders() }.bodyAsText()

    private fun io.ktor.client.request.HttpRequestBuilder.browserHeaders() {
        header(
            "User-Agent",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/130.0 Safari/537.36",
        )
        header("Accept-Language", "ja,en;q=0.8")
        // Without this the store hides its R18 titles behind an interstitial.
        cookie("wbadult", "1")
    }
}
