package com.moviesnight.tv.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.moviesnight.tv.ui.theme.Star

/**
 * Text with a large yellow quote mark hanging at its top-left, matching the
 * website's `.quate` class (pl-4, with a text-3xl yellow `"` at left-0 -top-1).
 */
@Composable
fun Quote(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
) {
    // The website pairs 18px text with a 30px mark; keep that ratio at any size.
    val markSize = style.fontSize * (30f / 18f)
    Box(modifier) {
        Text(
            "\"",
            style = style.copy(fontSize = markSize, lineHeight = markSize, fontWeight = FontWeight.Bold),
            color = Star,
            modifier = Modifier.offset(y = (-4).dp),
        )
        Text(text, style = style, color = color, modifier = Modifier.padding(start = 28.dp))
    }
}
