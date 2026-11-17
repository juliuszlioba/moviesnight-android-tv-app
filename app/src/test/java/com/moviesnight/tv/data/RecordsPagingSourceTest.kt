package com.moviesnight.tv.data

import androidx.paging.PagingSource
import com.moviesnight.tv.data.model.Rating
import com.moviesnight.tv.data.model.RecordSummary
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class RecordsPagingSourceTest {

    private val all = (0 until 100).map {
        RecordSummary(it.toLong(), "Title $it", 2000, null, Rating(it % 11), null)
    }

    private val source = RecordsPagingSource { offset, limit -> all.drop(offset).take(limit) }

    private fun load(key: Int?, size: Int) = runBlocking {
        source.load(PagingSource.LoadParams.Append(key ?: 0, size, false)) as PagingSource.LoadResult.Page
    }

    @Test
    fun `full page points to the next offset`() {
        val page = load(0, 42)
        assertEquals(42, page.data.size)
        assertEquals(42, page.nextKey)
        assertNull(page.prevKey)
    }

    @Test
    fun `short page ends paging`() {
        val page = load(84, 42)
        assertEquals(16, page.data.size)
        assertNull(page.nextKey)
    }

    @Test
    fun `fetch errors become LoadResult Error`() = runBlocking {
        val failing = RecordsPagingSource { _, _ -> throw IOException("offline") }
        val result = failing.load(PagingSource.LoadParams.Refresh(null, 42, false))
        assertTrue(result is PagingSource.LoadResult.Error)
    }
}
