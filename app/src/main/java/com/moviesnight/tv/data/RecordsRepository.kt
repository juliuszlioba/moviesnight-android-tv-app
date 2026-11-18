package com.moviesnight.tv.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.moviesnight.tv.data.model.MediaFilter
import com.moviesnight.tv.data.model.RecordDetail
import com.moviesnight.tv.data.model.RecordSummary
import com.moviesnight.tv.data.model.TierGroup
import com.moviesnight.tv.data.model.groupByTier
import com.moviesnight.tv.data.remote.DETAIL_COLUMNS
import com.moviesnight.tv.data.remote.RecordDetailDto
import com.moviesnight.tv.data.remote.RecordSummaryDto
import com.moviesnight.tv.data.remote.SUMMARY_COLUMNS
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.coroutines.flow.Flow

/** Read-only access to the `records` table. */
class RecordsRepository(private val supabase: SupabaseClient) {

    /**
     * Newest watched first (top lists: in list order), loaded [PAGE_SIZE] at a time
     * as the grid scrolls.
     */
    fun pagedRecords(filter: MediaFilter): Flow<PagingData<RecordSummary>> =
        pager { offset, limit -> page(filter, offset, limit) }

    /** Records whose title contains [query] (case-insensitive), newest watched first. */
    fun searchRecords(query: String): Flow<PagingData<RecordSummary>> =
        pager { offset, limit -> page(MediaFilter.All, offset, limit, titleQuery = query) }

    private fun pager(fetch: suspend (offset: Int, limit: Int) -> List<RecordSummary>) =
        Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                initialLoadSize = PAGE_SIZE,
                prefetchDistance = PAGE_SIZE / 2,
                enablePlaceholders = false,
            ),
            pagingSourceFactory = { RecordsPagingSource(fetch) },
        ).flow

    suspend fun page(
        filter: MediaFilter,
        offset: Int,
        limit: Int,
        titleQuery: String? = null,
    ): List<RecordSummary> =
        supabase.from(TABLE)
            .select(Columns.raw(SUMMARY_COLUMNS)) {
                filter {
                    when (filter) {
                        MediaFilter.All -> Unit
                        // anime live in the Anime tab only
                        MediaFilter.Movies -> {
                            eq("mediatype", "movie")
                            filter("sort_order_anime", FilterOperator.IS, "null")
                        }
                        MediaFilter.Series -> {
                            eq("mediatype", "series")
                            filter("sort_order_anime", FilterOperator.IS, "null")
                        }
                        MediaFilter.Anime, MediaFilter.AnimeTiers ->
                            filterNot("sort_order_anime", FilterOperator.IS, "null")
                        MediaFilter.TopMovies, MediaFilter.TopSeries, MediaFilter.TopAnime ->
                            filterNot(filter.topListColumn!!, FilterOperator.IS, "null")
                    }
                    if (!titleQuery.isNullOrBlank()) ilike("title", containsPattern(titleQuery))
                }
                val topList = filter.topListColumn
                if (topList != null) {
                    order(topList, Order.ASCENDING)
                    order("id", Order.ASCENDING)
                } else {
                    // id as tie-breaker keeps page boundaries stable for equal timestamps
                    order("watchedat", Order.DESCENDING, nullsFirst = false)
                    order("id", Order.DESCENDING)
                }
                range(offset.toLong(), (offset + limit - 1).toLong())
            }
            .decodeList<RecordSummaryDto>()
            .map { it.toModel() }

    /**
     * Every anime grouped by tier, each tier in anime-list order (`sort_order_anime`),
     * as on the website's /anime/tier. A few hundred rows, so it's loaded in one go.
     */
    suspend fun animeTierList(): List<TierGroup> {
        val records = mutableListOf<RecordSummary>()
        // PostgREST caps a response at max_rows (1000), so fetch in chunks.
        while (true) {
            val chunk = supabase.from(TABLE)
                .select(Columns.raw(SUMMARY_COLUMNS)) {
                    filter { filterNot("sort_order_anime", FilterOperator.IS, "null") }
                    order("sort_order_anime", Order.ASCENDING)
                    order("id", Order.ASCENDING)
                    range(records.size.toLong(), (records.size + TIER_CHUNK - 1).toLong())
                }
                .decodeList<RecordSummaryDto>()
                .map { it.toModel() }
            records += chunk
            if (chunk.size < TIER_CHUNK) break
        }
        return groupByTier(records)
    }

    suspend fun detail(id: Long): RecordDetail =
        supabase.from(TABLE)
            .select(Columns.raw(DETAIL_COLUMNS)) {
                filter { eq("id", id) }
            }
            .decodeSingle<RecordDetailDto>()
            .toModel()

    companion object {
        /** `%query%` for ILIKE, with the user's own `%`, `_` and `\` matched literally. */
        fun containsPattern(query: String): String =
            "%" + query.trim().replace(Regex("""[\\%_]""")) { "\\" + it.value } + "%"

        const val PAGE_SIZE = 42 // 7 full rows of the 6-column grid
        private const val TABLE = "records"
        private const val TIER_CHUNK = 1000
    }
}
