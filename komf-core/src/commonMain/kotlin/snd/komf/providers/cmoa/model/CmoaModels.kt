package snd.komf.providers.cmoa.model

import kotlin.jvm.JvmInline

@JvmInline
value class CmoaTitleId(val value: String) {
    override fun toString() = value

    val url get() = "https://www.cmoa.jp/title/$value/"
}

data class CmoaSearchResult(
    val id: CmoaTitleId,
    val title: String,
    /** The store's own genre - 少女マンガ, 青年マンガ, 小説, 文芸 - which is what separates
     *  a manga from the novel it was adapted from. */
    val genre: String?,
    val authors: List<String>,
    val thumbnailUrl: String?,
)

data class CmoaTitle(
    val id: CmoaTitleId,
    val title: String,
    val authors: List<String>,
    val summary: String?,
    val coverUrl: String?,
) {
    val url get() = id.url
}
