package com.moviesnight.tv.data

import com.moviesnight.tv.data.RecordsRepository.Companion.containsPattern
import org.junit.Assert.assertEquals
import org.junit.Test

class ContainsPatternTest {

    @Test
    fun `wraps the trimmed query in wildcards`() {
        assertEquals("%dune%", containsPattern("  dune "))
    }

    @Test
    fun `matches the user's own wildcards and backslashes literally`() {
        assertEquals("%100\\%%", containsPattern("100%"))
        assertEquals("%a\\_b%", containsPattern("a_b"))
        assertEquals("%c:\\\\x%", containsPattern("c:\\x"))
    }
}
