package snd.komf.providers.cmoa

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.cookie
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import snd.komf.model.Image
import snd.komf.providers.cmoa.model.CmoaSearchResult
import snd.komf.providers.cmoa.model.CmoaTitle
import snd.komf.providers.cmoa.model.CmoaTitleId

const val cmoaBaseUrl = "https://www.cmoa.jp"

/**
 * コミックシーモア (cmoa.jp).
 *
 * The broadest of the Japanese storefronts for this purpose: it carries digital-only
 * releases and adult titles that BOOK☆WALKER does not, and manga adaptations that
 * Bangumi and the NDL catalogue miss entirely.
 */
class CmoaClient(
    private val ktor: HttpClient,
) {
    private val parser = CmoaParser()

    suspend fun searchSeries(name: String): List<CmoaSearchResult> {
        val html = ktor.get("$cmoaBaseUrl/search/result/") {
            parameter("header_word", name)
            browserHeaders()
        }.bodyAsText()
        return parser.parseSearchResults(html)
    }

    suspend fun getTitle(
        id: CmoaTitleId,
        fallbackTitle: String,
    ): CmoaTitle = parser.parseTitle(id, ktor.get(id.url) { browserHeaders() }.bodyAsText(), fallbackTitle)

    suspend fun getThumbnail(url: String?): Image? {
        if (url == null) return null
        val bytes: ByteArray = ktor.get(url) { browserHeaders() }.body()
        return Image(bytes)
    }

    private fun io.ktor.client.request.HttpRequestBuilder.browserHeaders() {
        header(
            "User-Agent",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/130.0 Safari/537.36",
        )
        header("Accept-Language", "ja,en;q=0.8")
        // Without this the store hides its adult catalogue behind an interstitial.
        cookie("adult_check", "1")
    }
}
