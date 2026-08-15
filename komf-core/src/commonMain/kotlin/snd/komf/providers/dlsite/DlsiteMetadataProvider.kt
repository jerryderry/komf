package snd.komf.providers.dlsite

import snd.komf.model.Image
import snd.komf.model.MatchQuery
import snd.komf.model.ProviderBookId
import snd.komf.model.ProviderBookMetadata
import snd.komf.model.ProviderSeriesId
import snd.komf.model.ProviderSeriesMetadata
import snd.komf.model.SeriesSearchResult
import snd.komf.providers.CoreProviders
import snd.komf.providers.CoreProviders.DLSITE
import snd.komf.providers.MetadataProvider
import snd.komf.providers.dlsite.model.DlsiteProductId
import snd.komf.util.NameSimilarityMatcher

/**
 * DLsite (dlsite.com) - commercial digital doujinshi and comics.
 *
 * Fills the gap left by Hentag, whose API now answers 404. Coverage is partial
 * by nature: DLsite indexes works sold there, so event and scan releases are
 * frequently absent. It is worth having anyway because it is the only
 * doujinshi source that is both reachable and searchable - E-Hentai's API has
 * no search endpoint and its site is region-blocked.
 *
 * A DLsite product is a single standalone work, so there is no book hierarchy:
 * getBookMetadata has nothing meaningful to return.
 */
class DlsiteMetadataProvider(
    private val client: DlsiteClient,
    private val metadataMapper: DlsiteMetadataMapper,
    private val nameMatcher: NameSimilarityMatcher,
    private val fetchSeriesCovers: Boolean,
) : MetadataProvider {

    override fun providerName(): CoreProviders = DLSITE

    override suspend fun getSeriesMetadata(seriesId: ProviderSeriesId): ProviderSeriesMetadata {
        val product = client.getProduct(DlsiteProductId(seriesId.value))
            ?: throw IllegalStateException("DLsite product ${seriesId.value} could not be parsed")
        val thumbnail = if (fetchSeriesCovers) client.getThumbnail(product) else null
        return metadataMapper.toSeriesMetadata(product, thumbnail)
    }

    override suspend fun getSeriesCover(seriesId: ProviderSeriesId): Image? {
        val product = client.getProduct(DlsiteProductId(seriesId.value)) ?: return null
        return client.getThumbnail(product)
    }

    override suspend fun getBookMetadata(
        seriesId: ProviderSeriesId,
        bookId: ProviderBookId,
    ): ProviderBookMetadata {
        throw UnsupportedOperationException("DLsite products are standalone works with no volumes")
    }

    override suspend fun searchSeries(seriesName: String, limit: Int): Collection<SeriesSearchResult> {
        return client.searchSeries(seriesName.take(200))
            .take(limit)
            .map { metadataMapper.toSeriesSearchResult(it) }
    }

    override suspend fun matchSeriesMetadata(matchQuery: MatchQuery): ProviderSeriesMetadata? {
        val seriesName = matchQuery.seriesName
        // The listing does not always carry a usable title on the anchor, so
        // candidates without one are resolved before matching rather than
        // silently discarded.
        for (result in client.searchSeries(seriesName.take(200)).take(10)) {
            val title = result.title
            if (title != null && !nameMatcher.matches(seriesName, title)) continue

            val product = client.getProduct(result.id) ?: continue
            // For a serialised work the product is one chapter and its title ends in
            // the chapter number, so the series name is the one that can match a
            // folder. Accept either.
            val names = listOfNotNull(product.title, product.series)
            if (!nameMatcher.matches(seriesName, names)) continue

            val thumbnail = if (fetchSeriesCovers) client.getThumbnail(product) else null
            return metadataMapper.toSeriesMetadata(product, thumbnail)
        }
        return null
    }
}
