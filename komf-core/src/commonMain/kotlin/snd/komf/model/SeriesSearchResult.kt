package snd.komf.model

import kotlinx.serialization.Serializable
import snd.komf.providers.CoreProviders

@Serializable
data class SeriesSearchResult(
    val url: String?,
    val imageUrl: String? = null,
    val title: String,
    val provider: CoreProviders,
    val resultId: String,
    /**
     * Enough to tell two results apart when their titles do not. A search for a
     * title that exists as both a manga and a light novel, or as a series and its
     * individual volumes, otherwise returns rows that read identically.
     */
    val bookType: String? = null,
    val releaseDate: String? = null,
    val summary: String? = null,
)
