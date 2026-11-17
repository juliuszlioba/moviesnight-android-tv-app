package com.moviesnight.tv.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.moviesnight.tv.data.model.Rating
import com.moviesnight.tv.ui.theme.Star
import com.moviesnight.tv.ui.theme.StarEmpty

/** Same star shape as the website (Bootstrap Icons "star-fill", 16×16 viewport). */
private val StarPath = PathParser().parsePathString(
    "M3.612 15.443c-.386.198-.824-.149-.746-.592l.83-4.73L.173 6.765c-.329-.314-.158-.888.283-.95" +
        "l4.898-.696L7.538.792c.197-.39.73-.39.927 0l2.184 4.327 4.898.696c.441.062.612.636.282.95" +
        "l-3.522 3.356.83 4.73c.078.443-.36.79-.746.592L8 13.187l-4.389 2.256z",
).toPath()

private const val VIEWPORT = 16f

/** Five stars with halves. Unrated shows five empty stars. */
@Composable
fun StarRating(
    rating: Rating,
    modifier: Modifier = Modifier,
    starSize: Dp = 14.dp,
    spacing: Dp = 3.dp,
    filledColor: Color = Star,
    emptyColor: Color = StarEmpty,
) {
    val description = if (rating.isRated) "${rating.outOfTen / 2.0} of 5 stars" else "Not rated"
    Row(
        modifier = modifier.semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(spacing),
    ) {
        repeat(Rating.MAX_STARS) { index ->
            val fill = when {
                index < rating.fullStars -> 1f
                index == rating.fullStars && rating.hasHalfStar -> 0.5f
                else -> 0f
            }
            Canvas(Modifier.size(starSize)) {
                drawStar(emptyColor)
                if (fill > 0f) clipRect(right = size.width * fill) { drawStar(filledColor) }
            }
        }
    }
}

private fun DrawScope.drawStar(color: Color) {
    scale(size.width / VIEWPORT, size.height / VIEWPORT, pivot = Offset.Zero) {
        drawPath(StarPath, color)
    }
}
