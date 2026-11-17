package com.moviesnight.tv.data.remote

import com.moviesnight.tv.data.model.MediaType
import com.moviesnight.tv.data.model.Rating
import com.moviesnight.tv.data.model.RecordDetail
import com.moviesnight.tv.data.model.RecordSummary
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.OffsetDateTime

/** Columns requested for grid cards; keep in sync with [RecordSummaryDto]. */
internal const val SUMMARY_COLUMNS = "id,title,year,poster,rating,watchedat,tier"

/** All record columns plus genres through the records_ganres join table. */
internal const val DETAIL_COLUMNS =
    "id,mediatype,title,year,poster,rating,watchedat,recommendation,review,tier," +
        "sort_order_anime,records_ganres(ganres(title))"

@Serializable
internal data class RecordSummaryDto(
    val id: Long,
    val title: String,
    val year: Int,
    val poster: String? = null,
    val rating: Int? = null,
    val watchedat: String? = null,
    val tier: String? = null,
) {
    fun toModel() = RecordSummary(
        id = id,
        title = title,
        year = year,
        posterPath = poster?.takeIf { it.isNotBlank() },
        rating = Rating.of(rating),
        watchedAt = parseTimestamp(watchedat),
        tier = tier,
    )
}

@Serializable
internal data class RecordDetailDto(
    val id: Long,
    val mediatype: String,
    val title: String,
    val year: Int,
    val poster: String? = null,
    val rating: Int? = null,
    val watchedat: String? = null,
    val recommendation: String? = null,
    val review: String? = null,
    val tier: String? = null,
    @SerialName("sort_order_anime") val sortOrderAnime: Int? = null,
    @SerialName("records_ganres") val genreLinks: List<GenreLinkDto> = emptyList(),
) {
    fun toModel() = RecordDetail(
        id = id,
        mediaType = MediaType.from(mediatype),
        isAnime = sortOrderAnime != null,
        title = title,
        year = year,
        posterPath = poster?.takeIf { it.isNotBlank() },
        rating = Rating.of(rating),
        watchedAt = parseTimestamp(watchedat),
        genres = genreLinks.mapNotNull { it.genre?.title }.distinct().sorted(),
        recommendation = recommendation?.trim()?.takeIf { it.isNotEmpty() },
        review = review?.trim()?.takeIf { it.isNotEmpty() },
        tier = tier,
    )
}

@Serializable
internal data class GenreLinkDto(@SerialName("ganres") val genre: GenreDto? = null)

@Serializable
internal data class GenreDto(val title: String? = null)

/** Postgres timestamptz, e.g. "2026-03-14T17:38:45+00:00". */
internal fun parseTimestamp(value: String?): Instant? =
    value?.let { runCatching { OffsetDateTime.parse(it).toInstant() }.getOrNull() }
