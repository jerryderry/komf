package snd.komf.providers.cmoa

import snd.komf.model.Author
import snd.komf.model.AuthorRole
import snd.komf.model.Image
import snd.komf.model.ProviderSeriesId
import snd.komf.model.ProviderSeriesMetadata
import snd.komf.model.SeriesMetadata
import snd.komf.model.SeriesSearchResult
import snd.komf.model.SeriesTitle
import snd.komf.model.WebLink
import snd.komf.providers.CoreProviders.CMOA
import snd.komf.providers.MetadataConfigApplier
import snd.komf.providers.SeriesMetadataConfig
import snd.komf.providers.cmoa.model.CmoaSearchResult
import snd.komf.providers.cmoa.model.CmoaTitle

class CmoaMetadataMapper(
    private val seriesMetadataConfig: SeriesMetadataConfig,
    private val authorRoles: Collection<AuthorRole>,
    private val artistRoles: Collection<AuthorRole>,
) {
    fun toSeriesMetadata(
        title: CmoaTitle,
        thumbnail: Image?,
    ): ProviderSeriesMetadata {
        // The store credits everyone under 作家 without separating writer from artist.
        // For an adaptation that is two people - the novelist and whoever drew it - and
        // guessing which is which would be worse than crediting both for both.
        val authors = title.authors.flatMap { name ->
            (authorRoles + artistRoles).map { role -> Author(name, role) }
        }

        val metadata = SeriesMetadata(
            titles = listOf(SeriesTitle(title.title, null, "ja")),
            summary = title.summary,
            authors = authors,
            links = listOf(WebLink("コミックシーモア", title.url)),
            thumbnail = thumbnail,
        )

        return MetadataConfigApplier.apply(
            ProviderSeriesMetadata(id = ProviderSeriesId(title.id.value), metadata = metadata),
            seriesMetadataConfig,
        )
    }

    fun toSeriesSearchResult(result: CmoaSearchResult): SeriesSearchResult =
        SeriesSearchResult(
            url = result.id.url,
            imageUrl = result.thumbnailUrl,
            title = result.title,
            provider = CMOA,
            resultId = result.id.value,
            bookType = result.genre,
            summary = result.authors.joinToString("、").ifBlank { null },
        )
}
