package snd.komf.providers.bookwalkerjp

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
import snd.komf.providers.CoreProviders.BOOK_WALKER_JP
import snd.komf.providers.MetadataConfigApplier
import snd.komf.providers.SeriesMetadataConfig
import snd.komf.providers.bookwalkerjp.model.BookWalkerJpSearchResult
import snd.komf.providers.bookwalkerjp.model.BookWalkerJpSeries

private val dateRegex = "(\\d{4})/(\\d{1,2})/(\\d{1,2})".toRegex()

class BookWalkerJpMetadataMapper(
    private val seriesMetadataConfig: SeriesMetadataConfig,
    private val authorRoles: Collection<AuthorRole>,
    private val artistRoles: Collection<AuthorRole>,
) {
    fun toSeriesMetadata(
        series: BookWalkerJpSeries,
        thumbnail: Image?,
    ): ProviderSeriesMetadata {
        // The store credits "著者" without splitting writer from artist, which for the
        // manga it carries is usually one person doing both.
        val authors = series.authors.flatMap { name ->
            (authorRoles + artistRoles).map { role -> Author(name, role) }
        }

        val metadata = SeriesMetadata(
            titles = listOf(SeriesTitle(series.title, null, "ja")),
            summary = series.summary,
            publisher = series.publisher?.let { Publisher(it, ORIGINAL) },
            // The imprint is not a publisher, but it is the most useful thing the store
            // knows about where a series sits, so it goes in as a tag rather than lost.
            tags = listOfNotNull(series.label),
            authors = authors,
            releaseDate = series.releaseDate?.let { raw ->
                dateRegex.find(raw)?.let {
                    ReleaseDate(
                        it.groupValues[1].toIntOrNull(),
                        it.groupValues[2].toIntOrNull(),
                        it.groupValues[3].toIntOrNull(),
                    )
                }
            },
            links = listOf(WebLink("BOOK☆WALKER", series.url)),
            thumbnail = thumbnail,
        )

        return MetadataConfigApplier.apply(
            ProviderSeriesMetadata(id = ProviderSeriesId(series.id.value), metadata = metadata),
            seriesMetadataConfig,
        )
    }

    fun toSeriesSearchResult(
        result: BookWalkerJpSearchResult,
        category: String?,
    ): SeriesSearchResult =
        SeriesSearchResult(
            url = result.id.url,
            imageUrl = result.thumbnailUrl,
            title = result.title,
            provider = BOOK_WALKER_JP,
            resultId = result.id.value,
            bookType = category ?: result.label,
        )
}
