package snd.komf.providers.dlsite

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.encodeURLParameter
import snd.komf.model.Image
import snd.komf.providers.dlsite.model.DlsiteProduct
import snd.komf.providers.dlsite.model.DlsiteProductId
import snd.komf.providers.dlsite.model.DlsiteSearchResult

const val dlsiteBaseUrl = "https://www.dlsite.com"

class DlsiteClient(
    private val ktor: HttpClient,
) {
    private val parser = DlsiteParser()

    /**
     * Restricted to work_type MNG - DLsite sells games, voice works and manga
     * from the same catalogue, and an unfiltered keyword search buries the
     * comics under hundreds of the others.
     */
    suspend fun searchSeries(name: String): List<DlsiteSearchResult> {
        val keyword = name.encodeURLParameter()
        val url = "$dlsiteBaseUrl/maniax/fsr/=/language/jp/work_type%5B0%5D/MNG" +
            "/keyword/$keyword/order/trend"
        return parser.parseSearchResults(ktor.get(url) { browserHeaders() }.bodyAsText())
    }

    suspend fun getProduct(id: DlsiteProductId): DlsiteProduct? =
        parser.parseProduct(id, ktor.get(id.url) { browserHeaders() }.bodyAsText())

    suspend fun getThumbnail(product: DlsiteProduct): Image? {
        val url = product.coverUrl ?: return null
        val bytes: ByteArray = ktor.get(url) { browserHeaders() }.body()
        return Image(bytes)
    }

    private fun io.ktor.client.request.HttpRequestBuilder.browserHeaders() {
        header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/130.0 Safari/537.36")
        header("Accept-Language", "ja,en;q=0.8")
    }
}
