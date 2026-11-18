package com.moviesnight.tv.data.model

/**
 * What a home screen grid shows. The UI groups these into categories and views
 * (`ui/home/HomeCategory`).
 *
 * There is no `mediatype = 'anime'` in the database: like the website, a record
 * counts as anime when it has a position in the anime list (`sort_order_anime`).
 * Movies and Series leave anime out; they appear only under All and the anime filters.
 *
 * The Top filters show only records with a place in that top list ([topListColumn]
 * not null), in list order, like the website's sorted pages. Every other grid is
 * newest watched first.
 *
 * [AnimeTiers] is not a paged grid: it shows the same anime grouped by tier.
 */
enum class MediaFilter(val topListColumn: String? = null) {
    All,
    Movies,
    Series,
    Anime,
    TopMovies(topListColumn = "sort_order_movie"),
    TopSeries(topListColumn = "sort_order_series"),
    TopAnime(topListColumn = "sort_order_anime"),
    AnimeTiers,
}
