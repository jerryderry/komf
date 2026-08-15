package snd.komf.providers.cmoa

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import snd.komf.providers.cmoa.model.CmoaSearchResult
import snd.komf.providers.cmoa.model.CmoaTitle
import snd.komf.providers.cmoa.model.CmoaTitleId

private val titleIdRegex = "/title/(\\d+)/".toRegex()
private val json = Json { ignoreUnknownKeys = true; isLenient = true }

class CmoaParser {
    /**
     * The listing is richer than the product page here: it carries the genre and the
     * credits inline, so a search needs no follow-up request to say whether a hit is
     * the manga or the novel of the same name.
     */
    fun parseSearchResults(html: String): List<CmoaSearchResult> {
        val document = Ksoup.parse(html)
        val results = LinkedHashMap<String, CmoaSearchResult>()

        for (box in document.select("li.search_result_box")) {
            // Each result links to itself twice - once wrapping the cover image, once
            // wrapping the title - so the anchor has to be chosen by carrying text, not
            // merely by pointing at a title. Testing the href in Kotlin rather than with
            // a selector regex, which is fragile once slashes are involved.
            val link = box.select("a.title")
                .firstOrNull { titleIdRegex.containsMatchIn(it.attr("href")) && it.text().isNotBlank() }
                ?: continue
            val id = titleIdRegex.find(link.attr("href"))!!.groupValues[1]
            if (results.containsKey(id)) continue

            val title = link.text().trim()
            results[id] = CmoaSearchResult(
                id = CmoaTitleId(id),
                title = title,
                genre = box.select("a[href*=/search/genre/]").firstOrNull()?.text()?.trim()?.ifBlank { null },
                authors = box.select("a[href*=/search/author/]").map { it.text().trim() }.filter { it.isNotBlank() },
                thumbnailUrl = box.selectFirst("img.volume_img")?.attr("src")?.trim()?.ifBlank { null },
            )
        }
        return results.values.toList()
    }

    fun parseTitle(
        id: CmoaTitleId,
        html: String,
        fallbackTitle: String,
    ): CmoaTitle {
        val document = Ksoup.parse(html)
        val book = bookLinkedData(document)

        return CmoaTitle(
            id = id,
            title = book?.get("name")?.jsonPrimitive?.contentOrNull()?.trim()?.ifBlank { null } ?: fallbackTitle,
            authors = (book?.get("author") as? JsonArray)
                ?.mapNotNull { (it as? JsonObject)?.get("name")?.jsonPrimitive?.contentOrNull()?.trim() }
                ?.filter { it.isNotBlank() }
                ?: emptyList(),
            // The store prefixes its synopsis with its own marketing line; drop it.
            summary = ogContent(document, "og:description")
                ?.substringAfter("｜", "")
                ?.ifBlank { ogContent(document, "og:description") }
                ?.trim(),
            coverUrl = ogContent(document, "og:image")?.trim()?.ifBlank { null },
        )
    }

    /** The page carries several ld+json blocks; only one of them is the Book. */
    private fun bookLinkedData(document: Document): JsonObject? =
        document.select("script[type=application/ld+json]")
            .asSequence()
            .mapNotNull { runCatching { json.parseToJsonElement(it.data()) }.getOrNull() }
            .mapNotNull { it as? JsonObject }
            .firstOrNull { it["@type"]?.jsonPrimitive?.contentOrNull() == "Book" }

    private fun ogContent(
        document: Document,
        property: String,
    ) = document.selectFirst("meta[property=$property]")?.attr("content")?.ifBlank { null }
}

private fun kotlinx.serialization.json.JsonPrimitive.contentOrNull(): String? = if (isString) content else content.ifBlank { null }
