package snd.komf.providers.bookwalkerjp.model

import kotlin.jvm.JvmInline

@JvmInline
value class BookWalkerJpSeriesId(val value: String) {
    override fun toString() = value

    val url get() = "https://bookwalker.jp/series/$value/list/"
}

data class BookWalkerJpSearchResult(
    val id: BookWalkerJpSeriesId,
    val title: String,
    /** The imprint, e.g. フラワーコミックス. Present on the listing, unlike the category. */
    val label: String?,
    val thumbnailUrl: String?,
)

data class BookWalkerJpSeries(
    val id: BookWalkerJpSeriesId,
    val title: String,
    /** マンガ（漫画）, ライトノベル, 文芸・小説 - the store's own classification. */
    val category: String?,
    val authors: List<String>,
    val publisher: String?,
    val label: String?,
    val summary: String?,
    val coverUrl: String?,
    /** yyyy/MM/dd as the store writes it. */
    val releaseDate: String?,
) {
    val url get() = id.url
}
