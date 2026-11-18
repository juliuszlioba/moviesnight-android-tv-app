package com.moviesnight.tv.ui.home

import com.moviesnight.tv.data.model.MediaFilter

/**
 * The home screen's top tabs. Each category has one or more views (the second row of tabs),
 * and each view is one [MediaFilter] query. The first view is the default.
 */
enum class HomeCategory(val label: String, val views: List<MediaFilter>) {
    All("All", listOf(MediaFilter.All)),
    Movies("Movies", listOf(MediaFilter.Movies, MediaFilter.TopMovies)),
    Series("Series", listOf(MediaFilter.Series, MediaFilter.TopSeries)),
    Anime("Anime", listOf(MediaFilter.Anime, MediaFilter.TopAnime, MediaFilter.AnimeTiers)),
}

val MediaFilter.category: HomeCategory
    get() = HomeCategory.entries.first { this in it.views }

/** The label of this filter in its category's view tabs. */
val MediaFilter.viewLabel: String
    get() = when (this) {
        MediaFilter.All, MediaFilter.Movies, MediaFilter.Series, MediaFilter.Anime -> "Recent"
        MediaFilter.TopMovies, MediaFilter.TopSeries, MediaFilter.TopAnime -> "Top"
        MediaFilter.AnimeTiers -> "Tiers"
    }
