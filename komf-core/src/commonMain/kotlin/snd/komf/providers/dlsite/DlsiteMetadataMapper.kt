package snd.komf.providers.dlsite

import snd.komf.model.Author
import snd.komf.model.AuthorRole
import snd.komf.model.Image
import snd.komf.model.Publisher
import snd.komf.model.PublisherType.ORIGINAL
import snd.komf.model.ProviderSeriesId
import snd.komf.model.ProviderSeriesMetadata
import snd.komf.model.ReleaseDate
import snd.komf.model.SeriesMetadata
import snd.komf.model.SeriesSearchResult
import snd.komf.model.SeriesTitle
import snd.komf.model.WebLink
import snd.komf.providers.CoreProviders.DLSITE
import snd.komf.providers.MetadataConfigApplier
import snd.komf.providers.SeriesMetadataConfig
import snd.komf.providers.dlsite.model.DlsiteProduct
import snd.komf.providers.dlsite.model.DlsiteSearchResult

class DlsiteMetadataMapper(
    private val seriesMetadataConfig: SeriesMetadataConfig,
    private val authorRoles: Collection<AuthorRole>,
    private val artistRoles: Collection<AuthorRole>,
) {

    fun toSeriesMetadata(product: DlsiteProduct, thumbnail: Image?): ProviderSeriesMetadata {
        // DLsite does not split writer from artist for doujin works - one circle
        // is credited for the whole thing - so the same name fills both role sets
        // rather than guessing which half it belongs to.
        val authors = product.circle?.let { circle ->
            (authorRoles + artistRoles).map { role -> Author(circle, role) }
        } ?: emptyList()

        val metadata = SeriesMetadata(
            titles = listOf(SeriesTitle(product.title, null, "ja")),
            summary = product.summary,
            publisher = product.circle?.let { Publisher(it, ORIGINAL) },
            tags = product.genres,
            authors = authors,
            releaseDate = product.releaseDate?.let {
                ReleaseDate(it.year, it.monthNumber, it.dayOfMonth)
            },
            links = listOf(WebLink("DLsite", product.url)),
            // DLsite states this explicitly rather than leaving it to be inferred
            // from the tags, which is the one thing it does better than the
            // alternatives for this material.
            ageRating = if (product.ageRating?.startsWith("R18") == true) 18 else null,
            thumbnail = thumbnail,
        )

        return MetadataConfigApplier.apply(
            ProviderSeriesMetadata(
                id = ProviderSeriesId(product.id.value),
                metadata = metadata,
            ),
            seriesMetadataConfig
        )
    }

    fun toSeriesSearchResult(result: DlsiteSearchResult): SeriesSearchResult {
        return SeriesSearchResult(
            url = "https://www.dlsite.com/maniax/work/=/product_id/${result.id.value}.html",
            imageUrl = result.thumbnailUrl,
            title = result.title ?: result.id.value,
            provider = DLSITE,
            resultId = result.id.value,
        )
    }
}
