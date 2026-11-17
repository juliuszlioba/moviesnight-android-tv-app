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
    /** In the movie or series top list (not the anime one); shows the thumbs-up sticker. */
    val highlyRecommended: Boolean = false,
    /** Has a written review; shows a small speech-bubble next to the stars. */
    val hasReview: Boolean = false,
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
    /** In the movie or series top list (not the anime one). */
    val highlyRecommended: Boolean = false,
)

enum class MediaType { Movie, Series;
    companion object {
        fun from(value: String): MediaType = if (value == "series") Series else Movie
    }
}
