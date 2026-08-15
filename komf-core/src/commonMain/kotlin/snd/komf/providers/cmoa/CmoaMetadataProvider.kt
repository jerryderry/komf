package snd.komf.providers.cmoa

import snd.komf.model.Image
import snd.komf.model.MatchQuery
import snd.komf.model.ProviderBookId
import snd.komf.model.ProviderBookMetadata
import snd.komf.model.ProviderSeriesId
import snd.komf.model.ProviderSeriesMetadata
import snd.komf.model.SeriesSearchResult
import snd.komf.providers.CoreProviders
import snd.komf.providers.CoreProviders.CMOA
import snd.komf.providers.MetadataProvider
import snd.komf.providers.cmoa.model.CmoaTitleId
import snd.komf.util.NameSimilarityMatcher

class CmoaMetadataProvider(
    private val client: CmoaClient,
    private val metadataMapper: CmoaMetadataMapper,
    private val nameMatcher: NameSimilarityMatcher,
    private val fetchSeriesCovers: Boolean,
) : MetadataProvider {
    override fun providerName(): CoreProviders = CMOA

    override suspend fun getSeriesMetadata(seriesId: ProviderSeriesId): ProviderSeriesMetadata {
        val title = client.getTitle(CmoaTitleId(seriesId.value), seriesId.value)
        val thumbnail = if (fetchSeriesCovers) client.getThumbnail(title.coverUrl) else null
        return metadataMapper.toSeriesMetadata(title, thumbnail)
    }

    override suspend fun getSeriesCover(seriesId: ProviderSeriesId): Image? {
        val title = client.getTitle(CmoaTitleId(seriesId.value), seriesId.value)
        return client.getThumbnail(title.coverUrl)
    }

    override suspend fun getBookMetadata(
        seriesId: ProviderSeriesId,
        bookId: ProviderBookId,
    ): ProviderBookMetadata = throw UnsupportedOperationException("cmoa volumes are not matched individually")

    override suspend fun searchSeries(
        seriesName: String,
        limit: Int,
    ): Collection<SeriesSearchResult> =
        client.searchSeries(seriesName)
            .take(limit)
            .map { metadataMapper.toSeriesSearchResult(it) }

    override suspend fun matchSeriesMetadata(matchQuery: MatchQuery): ProviderSeriesMetadata? {
        val seriesName = matchQuery.seriesName
        for (result in client.searchSeries(seriesName).take(5)) {
            if (!nameMatcher.matches(seriesName, result.title)) continue

            val title = client.getTitle(result.id, result.title)
            val thumbnail = if (fetchSeriesCovers) client.getThumbnail(title.coverUrl) else null
            return metadataMapper.toSeriesMetadata(title, thumbnail)
        }
        return null
    }
}
