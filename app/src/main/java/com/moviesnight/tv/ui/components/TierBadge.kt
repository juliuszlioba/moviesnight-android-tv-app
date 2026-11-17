package com.moviesnight.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
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

/** Tailwind colors used by the website's TIER_STYLES (lib/tiers.ts). */
private data class TierStyle(val background: Color, val text: Color)

private val White = Color(0xFFFFFFFF)
private val TierStyles = mapOf(
    "S" to TierStyle(Color(0xFFEF4444), White),             // red-500
    "A" to TierStyle(Color(0xFFF97316), White),             // orange-500
    "B" to TierStyle(Color(0xFFFACC15), Color(0xFF171717)), // yellow-400, neutral-900 text
    "C" to TierStyle(Color(0xFF16A34A), White),             // green-600
    "D" to TierStyle(Color(0xFF3B82F6), White),             // blue-500
    "F" to TierStyle(Color(0xFF9333EA), White),             // purple-600
)

/** Sticker sizes: grid cards and the larger detail-screen poster. */
enum class TierBadgeSize(
    val fontSize: TextUnit,
    val padH: Dp,
    val padV: Dp,
    val corner: Dp,
    val shadow: Dp,
    /** Where the sticker sits relative to the poster's top-left corner (website: -left-2 top-3). */
    val offsetX: Dp,
    val offsetY: Dp,
) {
    Small(fontSize = 14.sp, padH = 10.dp, padV = 2.dp, corner = 5.dp, shadow = 8.dp, offsetX = (-7).dp, offsetY = 10.dp),
    Large(fontSize = 24.sp, padH = 16.dp, padV = 4.dp, corner = 8.dp, shadow = 14.dp, offsetX = (-12).dp, offsetY = 20.dp),
}

/**
 * Tilted tier sticker like the website's TierBadge. Renders nothing for
 * records without a tier (or with an unknown value).
 */
@Composable
fun TierBadge(tier: String?, size: TierBadgeSize, modifier: Modifier = Modifier) {
    val style = TierStyles[tier] ?: return
    val shape = RoundedCornerShape(size.corner)
    Text(
        text = tier!!,
        style = MaterialTheme.typography.labelLarge.copy(
            fontSize = size.fontSize,
            lineHeight = size.fontSize,
            fontWeight = FontWeight.Bold,
        ),
        color = style.text,
        modifier = modifier
            .rotate(-6f)
            // shadow-lg tinted with the badge color at 50%
            .shadow(size.shadow, shape, ambientColor = style.background.copy(alpha = 0.5f), spotColor = style.background.copy(alpha = 0.5f))
            .background(style.background, shape)
            .padding(horizontal = size.padH, vertical = size.padV),
    )
}
