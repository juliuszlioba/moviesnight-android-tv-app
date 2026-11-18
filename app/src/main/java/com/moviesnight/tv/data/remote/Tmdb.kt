package com.moviesnight.tv.data.remote

import com.moviesnight.tv.BuildConfig
import com.moviesnight.tv.data.model.MediaType
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** TMDB image CDN. Posters are stored as paths like "/abc123.jpg". */
object TmdbImage {
    private const val BASE = "https://image.tmdb.org/t/p/"

    /** Grid card (~200dp wide at 1080p). */
    fun gridPoster(path: String) = "${BASE}w342$path"

    /** Detail screen. */
    fun largePoster(path: String) = "${BASE}w780$path"
}

/** TMDB API v3, as used by the website. Only the details endpoints, for the description. */
class TmdbApi(
    private val apiKey: String = BuildConfig.TMDB_API_KEY,
    private val http: HttpClient = HttpClient(OkHttp),
) {
    /**
     * The movie's or series' overview from `/movie/{id}` or `/tv/{id}`, or null when
     * there's no API key or TMDB has no description. Throws on network/HTTP errors.
     */
    suspend fun overview(tmdbId: Int, mediaType: MediaType): String? {
        if (apiKey.isBlank()) return null
        val kind = if (mediaType == MediaType.Series) "tv" else "movie"
        val response = http.get("$BASE/$kind/$tmdbId") {
            parameter("api_key", apiKey)
            parameter("language", "en-US")
        }
        check(response.status.isSuccess()) { "TMDB $kind/$tmdbId: ${response.status}" }
        return parseOverview(response.bodyAsText())
    }

    companion object {
        private const val BASE = "https://api.themoviedb.org/3"
        private val json = Json { ignoreUnknownKeys = true }

        internal fun parseOverview(body: String): String? =
            json.decodeFromString<TmdbDetailsDto>(body).overview?.trim()?.takeIf { it.isNotEmpty() }
    }
}

@Serializable
internal data class TmdbDetailsDto(val overview: String? = null)
