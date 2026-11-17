package com.moviesnight.tv.data.remote

import com.moviesnight.tv.data.model.MediaType
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** Payloads shaped like real PostgREST responses from the records table. */
class RecordDtoTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `decodes a grid row`() {
        val row = json.decodeFromString<RecordSummaryDto>(
            """{"id":1204,"title":"GNOSIA","year":2025,"poster":"/dlQHZqADmu6mEsNUo2eA4FffAMt.jpg","rating":9,"watchedat":"2026-03-14T17:38:45+00:00","tier":"A"}""",
        ).toModel()

        assertEquals(1204L, row.id)
        assertEquals("/dlQHZqADmu6mEsNUo2eA4FffAMt.jpg", row.posterPath)
        assertEquals(9, row.rating.outOfTen)
        assertEquals(Instant.parse("2026-03-14T17:38:45Z"), row.watchedAt)
        assertEquals("A", row.tier)
    }

    @Test
    fun `tolerates missing poster, rating and fractional timestamps`() {
        val row = json.decodeFromString<RecordSummaryDto>(
            """{"id":1,"title":"X","year":2000,"poster":"","rating":null,"watchedat":"2026-03-14T17:38:45.953858+00:00"}""",
        ).toModel()

        assertNull(row.posterPath)
        assertFalse(row.rating.isRated)
        assertEquals(Instant.parse("2026-03-14T17:38:45.953858Z"), row.watchedAt)
    }

    @Test
    fun `decodes detail with genres through the join table`() {
        val detail = json.decodeFromString<RecordDetailDto>(
            """
            {"id":1203,"mediatype":"series","title":"Shelter","year":2026,"poster":"/p.jpg","rating":8,
             "watchedat":"2026-03-07T14:28:38+00:00","recommendation":" Good action movie ","review":"  ",
             "tier":null,"sort_order_anime":71,
             "records_ganres":[{"ganres":{"title":"Thriller"}},{"ganres":{"title":"Action"}},{"ganres":null}]}
            """.trimIndent(),
        ).toModel()

        assertEquals(MediaType.Series, detail.mediaType)
        assertTrue(detail.isAnime)
        assertEquals(listOf("Action", "Thriller"), detail.genres)
        assertEquals("Good action movie", detail.recommendation)
        assertNull(detail.review)
    }
}
