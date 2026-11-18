package com.moviesnight.tv.data.model

/**
 * The home screen tabs.
 *
 * There is no `mediatype = 'anime'` in the database: like the website, a record
 * counts as anime when it has a position in the anime list (`sort_order_anime`).
 * Movies and Series leave anime out; they appear only under All and the anime tabs.
 *
 * The Top tabs show only records with a place in that top list ([topListColumn]
 * not null), in list order, like the website's sorted pages. Every other grid is
 * newest watched first.
 *
 * [AnimeTiers] is not a paged grid: it shows the same anime grouped by tier.
 */
enum class MediaFilter(val label: String, val topListColumn: String? = null) {
    All("All"),
    Movies("Movies"),
    Series("Series"),
    Anime("Anime"),
    TopMovies("Top Movies", topListColumn = "sort_order_movie"),
    TopSeries("Top Series", topListColumn = "sort_order_series"),
    TopAnime("Top Anime", topListColumn = "sort_order_anime"),
    AnimeTiers("Anime Tier List"),
}
