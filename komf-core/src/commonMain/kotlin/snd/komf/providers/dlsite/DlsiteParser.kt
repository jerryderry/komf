package snd.komf.providers.dlsite

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import kotlinx.datetime.LocalDate
import snd.komf.providers.dlsite.model.DlsiteProduct
import snd.komf.providers.dlsite.model.DlsiteProductId
import snd.komf.providers.dlsite.model.DlsiteSearchResult

private val productIdRegex = "product_id/(RJ\\d+)\\.html".toRegex()
private val japaneseDateRegex = "(\\d{4})年(\\d{1,2})月(\\d{1,2})日".toRegex()
private val digitsRegex = "(\\d+)".toRegex()

class DlsiteParser {

    fun parseSearchResults(html: String): List<DlsiteSearchResult> {
        val document = Ksoup.parse(html)
        val results = LinkedHashMap<String, DlsiteSearchResult>()

        for (link in document.select("a[href*=product_id/]")) {
            val id = productIdRegex.find(link.attr("href"))?.groupValues?.get(1) ?: continue
            if (results.containsKey(id)) continue

            // The listing repeats each product as an image link and a text link;
            // take whichever carries a usable title and keep the first thumbnail.
            val title = link.attr("title").ifBlank { link.text() }.trim().ifBlank { null }
            val thumb = link.selectFirst("img")
                ?.let { it.attr("data-src").ifBlank { it.attr("src") } }
                ?.let { normaliseUrl(it) }

            results[id] = DlsiteSearchResult(DlsiteProductId(id), title, thumb)
        }
        return results.values.toList()
    }

    fun parseProduct(id: DlsiteProductId, html: String): DlsiteProduct? {
        val document = Ksoup.parse(html)
        val title = document.selectFirst("h1#work_name")?.text()?.trim()
            ?: return null

        val outline = parseOutline(document)

        return DlsiteProduct(
            id = id,
            title = title,
            // Not every product carries a circle row in the outline table; some
            // expose it only through the maker_name element beside the title.
            circle = outline["サークル名"] ?: outline["作者"]
                ?: document.selectFirst(".maker_name")?.text()?.trim()?.ifBlank { null },
            series = outline["シリーズ名"],
            genres = outline["ジャンル"]?.split(" ", "/")
                ?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList(),
            releaseDate = outline["販売日"]?.let { parseJapaneseDate(it) },
            pageCount = outline["ページ数"]?.let { digitsRegex.find(it)?.value?.toIntOrNull() },
            ageRating = outline["年齢指定"],
            workFormat = outline["作品形式"],
            summary = document.selectFirst("meta[property=og:description]")
                ?.attr("content")?.trim()?.ifBlank { null },
            coverUrl = document.selectFirst("meta[property=og:image]")
                ?.attr("content")?.let { normaliseUrl(it) },
        )
    }

    private fun parseOutline(document: Document): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        for (row in document.select("table#work_outline tr")) {
            val key = row.selectFirst("th")?.text()?.trim() ?: continue
            val value = row.selectFirst("td")?.text()?.trim() ?: continue
            if (key.isNotBlank() && value.isNotBlank()) out.putIfAbsent(key, value)
        }
        return out
    }

    private fun parseJapaneseDate(raw: String): LocalDate? {
        val m = japaneseDateRegex.find(raw) ?: return null
        return runCatching {
            LocalDate(
                m.groupValues[1].toInt(),
                m.groupValues[2].toInt(),
                m.groupValues[3].toInt()
            )
        }.getOrNull()
    }

    /** DLsite emits protocol-relative image URLs. */
    private fun normaliseUrl(url: String) = if (url.startsWith("//")) "https:$url" else url
}
