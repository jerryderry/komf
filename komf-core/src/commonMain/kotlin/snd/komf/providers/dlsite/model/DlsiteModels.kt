package snd.komf.providers.dlsite.model

import kotlinx.datetime.LocalDate
import kotlin.jvm.JvmInline

@JvmInline
value class DlsiteProductId(val value: String) {
    override fun toString() = value
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
    val url get() = "https://www.dlsite.com/maniax/work/=/product_id/${id.value}.html"
}
