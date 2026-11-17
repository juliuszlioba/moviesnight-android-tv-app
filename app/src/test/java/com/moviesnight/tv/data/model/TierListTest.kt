package com.moviesnight.tv.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TierListTest {

    private fun record(id: Long, tier: String?) =
        RecordSummary(id = id, title = "#$id", year = 2020, posterPath = null, rating = Rating(0), watchedAt = null, tier = tier)

    @Test
    fun `groups best tier first, keeps list order, ungraded last, skips empty tiers`() {
        val records = listOf(
            record(1, "A"), record(2, "S"), record(3, null), record(4, "A"),
            record(5, "F"), record(6, "X"), record(7, "S"),
        )
        val groups = groupByTier(records)
        assertEquals(listOf("S", "A", "F", null), groups.map { it.tier })
        assertEquals(listOf(2L, 7L), groups[0].records.map { it.id })
        assertEquals(listOf(1L, 4L), groups[1].records.map { it.id })
        assertEquals(listOf(3L, 6L), groups[3].records.map { it.id })
    }

    @Test
    fun `no records gives no groups`() {
        assertEquals(emptyList<TierGroup>(), groupByTier(emptyList()))
    }
}
