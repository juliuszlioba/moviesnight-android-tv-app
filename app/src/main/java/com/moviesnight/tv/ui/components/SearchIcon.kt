package com.moviesnight.tv.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Outlined magnifier, drawn here so the app doesn't need the Material icons library. Tint it via Icon. */
val SearchIcon: ImageVector = ImageVector.Builder(
    name = "Search",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).path(
    stroke = SolidColor(Color.Black),
    strokeLineWidth = 2f,
    strokeLineCap = StrokeCap.Round,
) {
    // Lens: circle of radius 6.5 around (10.5, 10.5)
    moveTo(10.5f, 4f)
    arcToRelative(6.5f, 6.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = 0f, dy1 = 13f)
    arcToRelative(6.5f, 6.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = 0f, dy1 = -13f)
    close()
    // Handle
    moveTo(15.2f, 15.2f)
    lineTo(20f, 20f)
}.build()
