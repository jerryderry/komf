package snd.komf.providers.bookwalkerjp

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import snd.komf.providers.bookwalkerjp.model.BookWalkerJpSearchResult
import snd.komf.providers.bookwalkerjp.model.BookWalkerJpSeries
import snd.komf.providers.bookwalkerjp.model.BookWalkerJpSeriesId

// "チアフルデイズ（フラワーコミックス）(マンガ（漫画）)の電子書籍無料試し読みなら…" - the store
// puts the category in the last parenthesised group before の電子書籍, and the title keeps
// its own full-width parentheses, so the two cannot be told apart by bracket type alone.
private val seriesTitleRegex = "^(?<title>.+?)\\((?<category>[^()]+)\\)の電子書籍".toRegex()

class BookWalkerJpParser {
    fun parseSearchResults(html: String): List<BookWalkerJpSearchResult> {
        val document = Ksoup.parse(html)
        val results = LinkedHashMap<String, BookWalkerJpSearchResult>()

        for (item in document.select("div.m-book-item")) {
            // The results page pads itself with a "related searches" carousel built from
            // the same markup. Those carry a different analytics category, which is the
            // only thing distinguishing them from an actual hit.
            if (item.select("[data-ga-category=word_related_list]").isNotEmpty()) continue

            val id = item.selectFirst("[data-series-id]")?.attr("data-series-id")?.ifBlank { null } ?: continue
            if (results.containsKey(id)) continue

            val title = item.selectFirst("a[title]")?.attr("title")?.trim()?.ifBlank { null } ?: continue
            val image = item.selectFirst("img")
                ?.let { it.attr("data-original").ifBlank { it.attr("src") } }
                ?.trim()?.ifBlank { null }

            results[id] = BookWalkerJpSearchResult(
                id = BookWalkerJpSeriesId(id),
                title = title,
                label = item.selectFirst(".m-book-item__label")?.text()?.trim()?.ifBlank { null },
                thumbnailUrl = image,
            )
        }
        return results.values.toList()
    }

    /** The category and the volume list live on the series page; everything else does not. */
    fun parseSeriesPage(html: String): Pair<String?, List<String>> {
        val document = Ksoup.parse(html)
        val match = ogContent(document, "og:title")?.let { seriesTitleRegex.find(it) }
        val books = document.select("a[href~=^https://bookwalker\\.jp/de[0-9a-f-]+/$]")
            .map { it.attr("href") }
            .distinct()
        return match?.groups?.get("category")?.value?.trim() to books
    }

    /**
     * A series has no author of its own on this store - the credits live on each volume,
     * so the first one stands in for the series.
     */
    fun parseSeries(
        id: BookWalkerJpSeriesId,
        seriesHtml: String,
        bookHtml: String?,
        fallbackTitle: String,
    ): BookWalkerJpSeries {
        val (category, _) = parseSeriesPage(seriesHtml)
        val seriesDocument = Ksoup.parse(seriesHtml)
        val title = ogContent(seriesDocument, "og:title")
            ?.let { seriesTitleRegex.find(it)?.groups?.get("title")?.value?.trim() }
            ?: fallbackTitle

        val book = bookHtml?.let { Ksoup.parse(it) }
        val details = book?.let { readDetails(it) } ?: emptyMap()

        return BookWalkerJpSeries(
            id = id,
            title = title,
            category = category,
            authors = details["著者"]
                ?.split("、", ",", "/")
                ?.map { it.replace(Regex("\\(.*?\\)|（.*?）"), "").trim() }
                ?.filter { it.isNotBlank() }
                ?: emptyList(),
            publisher = details["出版社"],
            label = details["レーベル"],
            summary = book?.let { ogContent(it, "og:description") }?.trim()?.ifBlank { null },
            coverUrl = book?.let { ogContent(it, "og:image") }?.trim()?.ifBlank { null },
            releaseDate = details["配信開始日"],
        )
    }

    private fun readDetails(document: Document): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        for (dt in document.select("dt")) {
            val key = dt.text().trim()
            val value = dt.nextElementSibling()?.takeIf { it.tagName() == "dd" }?.text()?.trim()
            if (key.isNotBlank() && !value.isNullOrBlank()) out.putIfAbsent(key, value)
        }
        return out
    }

    private fun ogContent(
        document: Document,
        property: String,
    ) = document.selectFirst("meta[property=$property]")?.attr("content")?.ifBlank { null }
}
