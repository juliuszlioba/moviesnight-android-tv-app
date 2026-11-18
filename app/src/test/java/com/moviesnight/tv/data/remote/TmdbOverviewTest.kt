package com.moviesnight.tv.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TmdbOverviewTest {

    @Test
    fun `reads overview from a details response`() {
        val body = """{"id":693134,"title":"Dune: Part Two","overview":" Follow the mythic journey. ","genres":[{"id":878}]}"""
        assertEquals("Follow the mythic journey.", TmdbApi.parseOverview(body))
    }

    @Test
    fun `blank or missing overview is null`() {
        assertNull(TmdbApi.parseOverview("""{"id":1,"overview":""}"""))
        assertNull(TmdbApi.parseOverview("""{"id":1}"""))
    }
}
