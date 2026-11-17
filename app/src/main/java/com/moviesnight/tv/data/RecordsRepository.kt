package com.moviesnight.tv.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.moviesnight.tv.data.model.MediaFilter
import com.moviesnight.tv.data.model.RecordDetail
import com.moviesnight.tv.data.model.RecordSummary
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

    /** Newest watched first, loaded [PAGE_SIZE] at a time as the grid scrolls. */
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
                        MediaFilter.Movies -> eq("mediatype", "movie")
                        MediaFilter.Series -> eq("mediatype", "series")
                        MediaFilter.Anime -> filterNot("sort_order_anime", FilterOperator.IS, "null")
                    }
                    if (!titleQuery.isNullOrBlank()) ilike("title", containsPattern(titleQuery))
                }
                // id as tie-breaker keeps page boundaries stable for equal timestamps
                order("watchedat", Order.DESCENDING, nullsFirst = false)
                order("id", Order.DESCENDING)
                range(offset.toLong(), (offset + limit - 1).toLong())
            }
            .decodeList<RecordSummaryDto>()
            .map { it.toModel() }

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
    }
}
