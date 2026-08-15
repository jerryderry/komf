package snd.komf.providers.dlsite.model

import kotlinx.datetime.LocalDate
import kotlin.jvm.JvmInline

@JvmInline
value class DlsiteProductId(val value: String) {
    override fun toString() = value

    /**
     * DLsite splits its catalogue across sections and the two-letter prefix says
     * which one a work lives in: RJ is 同人 under /maniax, BJ is commercial comics
     * under /books, VJ is the 美少女ゲーム catalogue under /pro. Requesting a work
     * from the wrong section redirects rather than failing, but the redirect is
     * an avoidable round trip on a storefront we deliberately query slowly.
     */
    val section: String
        get() = when (value.take(2)) {
            "BJ" -> "books"
            "VJ" -> "pro"
            else -> "maniax"
        }

    val url get() = "https://www.dlsite.com/$section/work/=/product_id/$value.html"

    /** Free to derive from the prefix, and enough to tell a doujin work from a
     *  commercial release in a list of otherwise identical titles. */
    val catalogue: String
        get() = when (value.take(2)) {
            "BJ" -> "商業誌"
            "VJ" -> "ゲーム"
            else -> "同人誌"
        }
}

data class DlsiteSearchResult(
    val id: DlsiteProductId,
    val title: String?,
    val thumbnailUrl: String?,
)

data class DlsiteProduct(
    val id: DlsiteProductId,
    val title: String,
    /** Circle for doujin works; DLsite labels it 作者 for some work types. */
    val circle: String?,
    val series: String?,
    /** DLsite's ジャンル list. Doubles as the parody for doujin - a Touhou work
     *  carries 東方Project here - which is what makes this useful at all. */
    val genres: List<String>,
    val releaseDate: LocalDate?,
    val pageCount: Int?,
    val ageRating: String?,
    val workFormat: String?,
    val summary: String?,
    val coverUrl: String?,
) {
    val url get() = id.url
}
