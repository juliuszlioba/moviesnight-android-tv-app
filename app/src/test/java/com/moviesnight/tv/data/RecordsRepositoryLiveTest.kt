package com.moviesnight.tv.data

import com.moviesnight.tv.data.model.MediaFilter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

/**
 * Smoke test against the real Supabase database (read-only).
 * Skipped unless run with: gradlew testDebugUnitTest -Plive
 */
class RecordsRepositoryLiveTest {

    private lateinit var repo: RecordsRepository

    @Before
    fun setUp() {
        assumeTrue("live tests disabled", System.getProperty("live") == "true")
        repo = RecordsRepository(createAppSupabaseClient())
    }

    @Test
    fun `every tab returns rows sorted newest first`() = runBlocking {
        for (filter in MediaFilter.entries.filter { it.topListColumn == null }) {
            val page = repo.page(filter, offset = 0, limit = RecordsRepository.PAGE_SIZE)
            println("$filter: ${page.size} rows, first = ${page.firstOrNull()?.title}")
            assertEquals("$filter page size", RecordsRepository.PAGE_SIZE, page.size)
            val times = page.mapNotNull { it.watchedAt }
            assertEquals("$filter sorted", times.sortedDescending(), times)
        }
    }

    @Test
    fun `top list tabs return only listed records`() = runBlocking {
        for (filter in MediaFilter.entries.filter { it.topListColumn != null }) {
            val page = repo.page(filter, offset = 0, limit = RecordsRepository.PAGE_SIZE)
            println("$filter: ${page.size} rows, first = ${page.firstOrNull()?.title}")
            assertTrue("$filter not empty", page.isNotEmpty())
            if (filter != MediaFilter.TopAnime) {
                // movie/series list membership is what makes a record highly recommended
                assertTrue("$filter all listed", page.all { it.highlyRecommended })
            }
        }
    }

    @Test
    fun `pages do not overlap`() = runBlocking {
        val first = repo.page(MediaFilter.All, 0, 42).map { it.id }
        val second = repo.page(MediaFilter.All, 42, 42).map { it.id }
        assertTrue(first.intersect(second.toSet()).isEmpty())
    }

    @Test
    fun `paging reaches the end of the anime list`() = runBlocking {
        var offset = 0
        var fetched = 0
        val ids = mutableSetOf<Long>()
        while (true) {
            val page = repo.page(MediaFilter.Anime, offset, 100)
            ids += page.map { it.id }
            fetched += page.size
            if (page.size < 100) break
            offset += page.size
        }
        println("Anime total: ${ids.size}")
        assertEquals("no duplicates across pages", fetched, ids.size)
        assertTrue(ids.size > 100)
    }

    @Test
    fun `anime tier list is grouped best tier first`() = runBlocking {
        val groups = repo.animeTierList()
        groups.forEach { println("Tier ${it.tier ?: "?"}: ${it.records.size}, first = ${it.records.first().title}") }
        assertTrue(groups.isNotEmpty())
        assertEquals(listOf("S", "A", "B", "C", "D", "F", null).filter { t -> groups.any { it.tier == t } }, groups.map { it.tier })
        val ids = groups.flatMap { g -> g.records.map { it.id } }
        assertEquals("no duplicates", ids.size, ids.toSet().size)
    }

    @Test
    fun `detail includes genres`() = runBlocking {
        // id 1 is the first record ever added and has genres in records_ganres
        val detail = repo.detail(1)
        println("Detail: ${detail.title} (${detail.year}) genres=${detail.genres} rating=${detail.rating.outOfTen}")
        assertTrue(detail.genres.isNotEmpty())
    }
}
