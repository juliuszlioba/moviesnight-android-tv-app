package com.moviesnight.tv.data.remote

/** TMDB image CDN. Posters are stored as paths like "/abc123.jpg". */
object TmdbImage {
    private const val BASE = "https://image.tmdb.org/t/p/"

    /** Grid card (~200dp wide at 1080p). */
    fun gridPoster(path: String) = "${BASE}w342$path"

    /** Detail screen. */
    fun largePoster(path: String) = "${BASE}w780$path"
}
