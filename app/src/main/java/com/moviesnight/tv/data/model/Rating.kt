package com.moviesnight.tv.data.model

/**
 * A rating stored as 1–10 in the database and shown as five stars with halves,
 * the same mapping the website uses (9 → ★★★★½, 8 → ★★★★).
 * 0 or null means unrated.
 */
@JvmInline
value class Rating(val outOfTen: Int) {
    val isRated: Boolean get() = outOfTen in 1..10
    val fullStars: Int get() = outOfTen.coerceIn(0, 10) / 2
    val hasHalfStar: Boolean get() = isRated && outOfTen % 2 == 1
    val emptyStars: Int get() = 5 - fullStars - (if (hasHalfStar) 1 else 0)

    companion object {
        const val MAX_STARS = 5
        fun of(value: Int?): Rating = Rating(value ?: 0)
    }
}
