package com.moviesnight.tv.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import com.moviesnight.tv.ui.theme.StarEmpty

/** Lucide's message-circle, the icon the website uses for reviews. Tint it via Icon. */
val ReviewIcon: ImageVector = ImageVector.Builder(
    name = "MessageCircle",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).addPath(
    pathData = addPathNodes("M7.9 20A9 9 0 1 0 4 16.1L2 22Z"),
    stroke = SolidColor(Color.Black),
    strokeLineWidth = 2f,
    strokeLineCap = StrokeCap.Round,
    strokeLineJoin = StrokeJoin.Round,
).build()

/** Small speech bubble, empty-star grey, shown beside the stars when a record has a written review. */
@Composable
fun ReviewIndicator(modifier: Modifier = Modifier) {
    Icon(
        imageVector = ReviewIcon,
        contentDescription = "Has review",
        tint = StarEmpty,
        modifier = modifier.size(13.dp),
    )
}
