package com.moviesnight.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import com.moviesnight.tv.ui.preview.PreviewSurface
import com.moviesnight.tv.ui.preview.TvComponentPreview
import com.moviesnight.tv.ui.theme.Background
import com.moviesnight.tv.ui.theme.Star

/** Lucide's thumbs-up (the website's icon), stroke 1.5 as there. Tint it via Icon. */
val ThumbsUpIcon: ImageVector = ImageVector.Builder(
    name = "ThumbsUp",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    listOf(
        "M7 10v12",
        "M15 5.88 14 10h5.83a2 2 0 0 1 1.92 2.56l-2.33 8A2 2 0 0 1 17.5 22H4a2 2 0 0 1-2-2v-8a2 2 0 0 1 2-2h2.76a2 2 0 0 0 1.79-1.11L12 2a3.13 3.13 0 0 1 3 3.88Z",
    ).forEach {
        addPath(
            pathData = addPathNodes(it),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }
}.build()

/** Sticker sizes: grid cards and the larger detail-screen poster. */
enum class RecommendedBadgeSize(
    val icon: Dp,
    val pad: Dp,
    val corner: Dp,
    val shadow: Dp,
    /**
     * Where the sticker sits relative to the poster's top-left corner. It hangs over the
     * left edge in the tier sticker's spot, or just below it when there is one, so it never
     * covers the title shown at the bottom of a focused card.
     */
    val offsetX: Dp,
    val offsetY: Dp,
    val belowTierOffsetY: Dp,
) {
    // offsetY matches TierBadgeSize; belowTierOffsetY = that + tier sticker side + a small gap.
    Small(icon = 20.dp, pad = 5.dp, corner = 5.dp, shadow = 8.dp, offsetX = (-7).dp, offsetY = 10.dp, belowTierOffsetY = 48.dp),
    Large(icon = 32.dp, pad = 8.dp, corner = 8.dp, shadow = 14.dp, offsetX = (-12).dp, offsetY = 20.dp, belowTierOffsetY = 80.dp),
    ;

    fun offsetY(hasTier: Boolean): Dp = if (hasTier) belowTierOffsetY else offsetY
}

/**
 * Tilted yellow thumbs-up sticker like the website's "Highly recommended" badge.
 * Callers decide whether to show it ([com.moviesnight.tv.data.model.RecordSummary.highlyRecommended]).
 */
@Composable
fun RecommendedBadge(size: RecommendedBadgeSize, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(size.corner)
    val glow = Star.copy(alpha = 0.5f)
    Icon(
        imageVector = ThumbsUpIcon,
        contentDescription = "Highly recommended",
        tint = Background,
        modifier = modifier
            .rotate(-6f)
            // shadow-lg tinted yellow-500 at 50%
            .shadow(size.shadow, shape, ambientColor = glow, spotColor = glow)
            .background(Star, shape)
            .padding(size.pad)
            .size(size.icon),
    )
}

@TvComponentPreview
@Composable
private fun RecommendedBadgePreview() {
    PreviewSurface {
        Row(Modifier.padding(32.dp), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            RecommendedBadgeSize.entries.forEach { RecommendedBadge(it) }
        }
    }
}
