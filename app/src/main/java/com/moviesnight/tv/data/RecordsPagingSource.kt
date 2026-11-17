package com.moviesnight.tv.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.moviesnight.tv.data.model.RecordSummary
import kotlin.coroutines.cancellation.CancellationException

/** Offset-based, append-only paging over a sorted query. Keys are row offsets. */
class RecordsPagingSource(
    private val fetch: suspend (offset: Int, limit: Int) -> List<RecordSummary>,
) : PagingSource<Int, RecordSummary>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, RecordSummary> {
        val offset = params.key ?: 0
        return try {
            val items = fetch(offset, params.loadSize)
            LoadResult.Page(
                data = items,
                prevKey = null,
                nextKey = if (items.size < params.loadSize) null else offset + items.size,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    /** Refresh (e.g. retry) restarts from the top: newest records are what matter. */
    override fun getRefreshKey(state: PagingState<Int, RecordSummary>): Int? = null
}
