package com.moviesnight.tv.ui.preview

import android.graphics.drawable.GradientDrawable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import coil3.annotation.ExperimentalCoilApi
import coil3.asImage
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import com.moviesnight.tv.ui.theme.MoviesNightTheme
import kotlin.math.absoluteValue

/**
 * A full Shield screen: 1920×1080 px at xhdpi (320 dpi) is 960×540 dp, the same
 * size the app lays out for on the device and the TV emulator.
 */
@Preview(name = "Shield 1080p", device = "spec:width=960dp,height=540dp,dpi=320", showBackground = true, backgroundColor = 0xFF000000)
annotation class TvScreenPreview

/** A single component on the app background, sized to its content. */
@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 960)
annotation class TvComponentPreview

/**
 * App theme + background, like MainActivity. Posters can't be downloaded in a
 * preview, so each poster URL is drawn as its own color gradient instead.
 */
@OptIn(ExperimentalCoilApi::class)
@Composable
fun PreviewSurface(content: @Composable () -> Unit) {
    MoviesNightTheme {
        CompositionLocalProvider(LocalAsyncImagePreviewHandler provides FakePosters) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RectangleShape,
                colors = SurfaceDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                ),
                content = { content() },
            )
        }
    }
}

private val PosterColors = listOf(
    0xFF7F1D1D to 0xFF1C1917, // red
    0xFF1E3A8A to 0xFF0F172A, // blue
    0xFF065F46 to 0xFF022C22, // green
    0xFF92400E to 0xFF1C1917, // amber
    0xFF581C87 to 0xFF1E1B4B, // purple
    0xFF155E75 to 0xFF082F49, // cyan
    0xFF9D174D to 0xFF2E1065, // pink
    0xFF3F3F46 to 0xFF09090B, // grey
)

@OptIn(ExperimentalCoilApi::class)
private val FakePosters = AsyncImagePreviewHandler { request ->
    val (top, bottom) = PosterColors[request.data.hashCode().absoluteValue % PosterColors.size]
    GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        intArrayOf(top.toInt(), bottom.toInt()),
    ).asImage()
}
