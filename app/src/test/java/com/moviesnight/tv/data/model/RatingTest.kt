package com.moviesnight.tv.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RatingTest {

    private fun stars(r: Int): String = Rating(r).run {
        "★".repeat(fullStars) + (if (hasHalfStar) "½" else "") + "☆".repeat(emptyStars)
    }

    @Test
    fun `maps 1-10 to five stars with halves like the website`() {
        assertEquals("★★★★★", stars(10))
        assertEquals("★★★★½", stars(9))
        assertEquals("★★★★☆", stars(8))
        assertEquals("★★★½☆", stars(7))
        assertEquals("★½☆☆☆", stars(3))
        assertEquals("½☆☆☆☆", stars(1))
    }

    @Test
    fun `zero and null are unrated`() {
        assertFalse(Rating.of(null).isRated)
        assertFalse(Rating(0).isRated)
        assertEquals("☆☆☆☆☆", stars(0))
        assertTrue(Rating(1).isRated)
    }

    @Test
    fun `always five star slots`() {
        for (r in 0..10) {
            val rating = Rating(r)
            val slots = rating.fullStars + (if (rating.hasHalfStar) 1 else 0) + rating.emptyStars
            assertEquals("rating $r", Rating.MAX_STARS, slots)
        }
    }
}
