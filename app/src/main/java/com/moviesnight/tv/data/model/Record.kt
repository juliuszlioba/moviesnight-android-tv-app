package com.moviesnight.tv.data.model

import java.time.Instant

/** A grid card: only what the poster grid needs. */
data class RecordSummary(
    val id: Long,
    val title: String,
    val year: Int,
    val posterPath: String?,
    val rating: Rating,
    val watchedAt: Instant?,
    /** Tier-list grade S/A/B/C/D/F, or null. */
    val tier: String? = null,
)

/** Everything the detail screen shows. */
data class RecordDetail(
    val id: Long,
    val mediaType: MediaType,
    val isAnime: Boolean,
    val title: String,
    val year: Int,
    val posterPath: String?,
    val rating: Rating,
    val watchedAt: Instant?,
    val genres: List<String>,
    val recommendation: String?,
    val review: String?,
    val tier: String?,
)

enum class MediaType { Movie, Series;
    companion object {
        fun from(value: String): MediaType = if (value == "series") Series else Movie
    }
}
