package com.moviesnight.tv.data.model

/**
 * The home screen tabs.
 *
 * There is no `mediatype = 'anime'` in the database: like the website, a record
 * counts as anime when it has a position in the anime list (`sort_order_anime`).
 * Anime therefore also appear under Movies or Series.
 *
 * [AnimeTiers] is not a paged grid: it shows the same anime grouped by tier.
 */
enum class MediaFilter(val label: String) {
    All("All"),
    Movies("Movies"),
    Series("Series"),
    Anime("Anime"),
    AnimeTiers("Anime Tier List"),
}
