package com.moviesnight.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import com.moviesnight.tv.ui.preview.PreviewSurface
import com.moviesnight.tv.ui.preview.TvComponentPreview

/** Tailwind colors used by the website's TIER_STYLES (lib/tiers.ts). Text is always white. */
private val TierColors = mapOf(
    "S" to Color(0xFFEF4444), // red-500
    "A" to Color(0xFFF97316), // orange-500
    "B" to Color(0xFFFACC15), // yellow-400
    "C" to Color(0xFF16A34A), // green-600
    "D" to Color(0xFF3B82F6), // blue-500
    "F" to Color(0xFF9333EA), // purple-600
)

/** The website's color for [tier], or null for no tier or an unknown one. */
fun tierColor(tier: String?): Color? = TierColors[tier]

/** Sticker sizes: grid cards and the larger detail-screen poster. */
enum class TierBadgeSize(
    val fontSize: TextUnit,
    /** Square side; matches [RecommendedBadgeSize] (icon + padding on both sides). */
    val side: Dp,
    val corner: Dp,
    val shadow: Dp,
    /** Where the sticker sits relative to the poster's top-left corner (website: -left-2 top-3). */
    val offsetX: Dp,
    val offsetY: Dp,
) {
    Small(fontSize = 16.sp, side = 30.dp, corner = 5.dp, shadow = 8.dp, offsetX = (-7).dp, offsetY = 10.dp),
    Large(fontSize = 26.sp, side = 48.dp, corner = 8.dp, shadow = 14.dp, offsetX = (-12).dp, offsetY = 20.dp),
}

/**
 * Tilted square tier sticker, same shape as [RecommendedBadge]. Renders nothing for
 * records without a tier (or with an unknown value).
 */
@Composable
fun TierBadge(tier: String?, size: TierBadgeSize, modifier: Modifier = Modifier) {
    val color = TierColors[tier] ?: return
    val shape = RoundedCornerShape(size.corner)
    val glow = color.copy(alpha = 0.5f)
    Box(
        modifier = modifier
            .rotate(-6f)
            // shadow-lg tinted with the badge color at 50%
            .shadow(size.shadow, shape, ambientColor = glow, spotColor = glow)
            .background(color, shape)
            .size(size.side),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = tier!!,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = size.fontSize,
                lineHeight = size.fontSize,
                fontWeight = FontWeight.Bold,
            ),
            color = Color.White,
        )
    }
}

@TvComponentPreview
@Composable
private fun TierBadgePreview() {
    PreviewSurface {
        Column(Modifier.padding(32.dp), verticalArrangement = Arrangement.spacedBy(32.dp)) {
            TierBadgeSize.entries.forEach { size ->
                Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    TierColors.keys.forEach { TierBadge(it, size) }
                }
            }
        }
    }
}
