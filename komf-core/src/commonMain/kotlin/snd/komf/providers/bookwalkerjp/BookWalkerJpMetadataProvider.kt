package snd.komf.providers.bookwalkerjp

import snd.komf.model.Image
import snd.komf.model.MatchQuery
import snd.komf.model.ProviderBookId
import snd.komf.model.ProviderBookMetadata
import snd.komf.model.ProviderSeriesId
import snd.komf.model.ProviderSeriesMetadata
import snd.komf.model.SeriesSearchResult
import snd.komf.providers.CoreProviders
import snd.komf.providers.CoreProviders.BOOK_WALKER_JP
import snd.komf.providers.MetadataProvider
import snd.komf.providers.bookwalkerjp.model.BookWalkerJpSeriesId
import snd.komf.util.NameSimilarityMatcher

/**
 * BOOK☆WALKER's Japanese store (bookwalker.jp).
 *
 * Covers Japanese commercial manga and light novels that never reach the English
 * storefront the existing BookWalker provider queries, and that Bangumi's community
 * catalogue is patchy on. No API key and no age wall beyond a cookie.
 */
class BookWalkerJpMetadataProvider(
    private val client: BookWalkerJpClient,
    private val metadataMapper: BookWalkerJpMetadataMapper,
    private val nameMatcher: NameSimilarityMatcher,
    private val fetchSeriesCovers: Boolean,
) : MetadataProvider {
    override fun providerName(): CoreProviders = BOOK_WALKER_JP

    override suspend fun getSeriesMetadata(seriesId: ProviderSeriesId): ProviderSeriesMetadata {
        val series = client.getSeries(BookWalkerJpSeriesId(seriesId.value), seriesId.value)
        val thumbnail = if (fetchSeriesCovers) client.getThumbnail(series.coverUrl) else null
        return metadataMapper.toSeriesMetadata(series, thumbnail)
    }

    override suspend fun getSeriesCover(seriesId: ProviderSeriesId): Image? {
        val series = client.getSeries(BookWalkerJpSeriesId(seriesId.value), seriesId.value)
        return client.getThumbnail(series.coverUrl)
    }

    override suspend fun getBookMetadata(
        seriesId: ProviderSeriesId,
        bookId: ProviderBookId,
    ): ProviderBookMetadata = throw UnsupportedOperationException("BOOK☆WALKER volumes are not matched individually")

    override suspend fun searchSeries(
        seriesName: String,
        limit: Int,
    ): Collection<SeriesSearchResult> =
        client.searchSeries(seriesName)
            .take(limit)
            .map { result ->
                // The category is what separates a manga from the light novel of the same
                // name, and the listing does not carry it. Worth a request per result on
                // a search someone is watching.
                val category = runCatching { client.getCategory(result.id) }.getOrNull()
                metadataMapper.toSeriesSearchResult(result, category)
            }

    override suspend fun matchSeriesMetadata(matchQuery: MatchQuery): ProviderSeriesMetadata? {
        val seriesName = matchQuery.seriesName
        for (result in client.searchSeries(seriesName).take(5)) {
            // Listings append the imprint to the title - "チアフルデイズ（フラワーコミックス）" -
            // which no library folder carries, so match the bare title too.
            val bare = result.title.substringBefore("（").trim()
            if (!nameMatcher.matches(seriesName, listOf(result.title, bare))) continue

            val series = client.getSeries(result.id, result.title)
            val thumbnail = if (fetchSeriesCovers) client.getThumbnail(series.coverUrl) else null
            return metadataMapper.toSeriesMetadata(series, thumbnail)
        }
        return null
    }
}
