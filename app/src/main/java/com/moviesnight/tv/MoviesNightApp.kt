package com.moviesnight.tv

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.request.crossfade
import com.moviesnight.tv.data.RecordsRepository
import com.moviesnight.tv.data.createAppSupabaseClient
import com.moviesnight.tv.data.remote.TmdbApi

/** Holds app-wide singletons; small enough that a DI framework isn't worth it. */
class MoviesNightApp : Application(), SingletonImageLoader.Factory {

    val recordsRepository: RecordsRepository by lazy {
        RecordsRepository(createAppSupabaseClient())
    }

    val tmdbApi: TmdbApi by lazy { TmdbApi() }

    /** Posters fade in softly; memory + disk caches use Coil's defaults. */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .crossfade(true)
            .build()
}
