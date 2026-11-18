package com.moviesnight.tv.ui.preview

import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import com.moviesnight.tv.data.model.MediaType
import com.moviesnight.tv.data.model.Rating
import com.moviesnight.tv.data.model.RecordDetail
import com.moviesnight.tv.data.model.RecordSummary
import com.moviesnight.tv.data.model.TierGroup
import com.moviesnight.tv.data.model.groupByTier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.Instant
import java.time.temporal.ChronoUnit

/** Made-up records for Compose previews. Poster paths are fake; previews draw gradients for them. */
object SampleRecords {

    private val now: Instant = Instant.parse("2026-11-15T20:00:00Z")

    private val titles = listOf(
        "Dune: Part Two" to 2024, "Arcane" to 2021, "Spirited Away" to 2001,
        "The Bear" to 2022, "Past Lives" to 2023, "Frieren: Beyond Journey's End" to 2023,
        "Oppenheimer" to 2023, "Severance" to 2022, "Perfect Days" to 2023,
        "Shōgun" to 2024, "Your Name." to 2016, "Everything Everywhere All at Once" to 2022,
        "Andor" to 2022, "Anatomy of a Fall" to 2023, "The Boy and the Heron" to 2023,
        "Fallout" to 2024, "Poor Things" to 2023, "Blue Eye Samurai" to 2023,
    )
    private val tiers = listOf("S", null, "A", null, "B", null, null, "C", null, "D", null, "F")

    val summaries: List<RecordSummary> = titles.mapIndexed { i, (title, year) ->
        RecordSummary(
            id = i + 1L,
            title = title,
            year = year,
            // Every 7th record has no poster, to show the title tile.
            posterPath = if (i % 7 == 6) null else "/sample-poster-$i.jpg",
            rating = Rating(10 - i % 10),
            watchedAt = now.minus(i * 4L, ChronoUnit.DAYS),
            tier = tiers[i % tiers.size],
            // Every 3rd record is in a top list, to show the thumbs-up sticker.
            highlyRecommended = i % 3 == 0,
            hasReview = i % 2 == 0,
        )
    }

    /** Tier list sections: the summaries repeated so S and A wrap onto a second row. */
    val tierGroups: List<TierGroup> = groupByTier(
        (0 until 4).flatMap { copy ->
            summaries.map { it.copy(id = it.id + copy * 100L, tier = it.tier ?: if (copy < 2) "A" else null) }
        },
    )

    val detail = RecordDetail(
        id = 1,
        mediaType = MediaType.Movie,
        isAnime = false,
        title = "Dune: Part Two",
        year = 2024,
        posterPath = "/sample-poster-0.jpg",
        rating = Rating(9),
        watchedAt = now,
        genres = listOf("Science Fiction", "Adventure", "Drama"),
        recommendation = "See it on the biggest screen you can find.",
        review = List(6) {
            "Villeneuve picks up exactly where the first part stopped and somehow raises the scale " +
                "without losing the quiet moments. The sound design alone is worth the ticket, and " +
                "the desert has never looked this alien."
        }.joinToString("\n\n"),
        tier = "S",
        highlyRecommended = true,
    )

    /** TMDB-style overview for [detail]. */
    const val description =
        "Follow the mythic journey of Paul Atreides as he unites with Chani and the Fremen while " +
            "on a path of revenge against the conspirators who destroyed his family."

    /** Anime series with no poster, no tier, no recommendation and a short review. */
    val sparseDetail = RecordDetail(
        id = 6,
        mediaType = MediaType.Series,
        isAnime = true,
        title = "Frieren: Beyond Journey's End",
        year = 2023,
        posterPath = null,
        rating = Rating(0),
        watchedAt = null,
        genres = emptyList(),
        recommendation = null,
        review = "Gentle and melancholic.",
        tier = null,
    )

    fun paged(records: List<RecordSummary> = summaries): Flow<PagingData<RecordSummary>> =
        flowOf(PagingData.from(records))

    /** An empty list stuck in [refresh], for the loading, error and empty states. */
    fun pagedState(refresh: LoadState): Flow<PagingData<RecordSummary>> {
        val idle = LoadState.NotLoading(endOfPaginationReached = true)
        return flowOf(PagingData.from(emptyList(), LoadStates(refresh = refresh, prepend = idle, append = idle)))
    }
}
