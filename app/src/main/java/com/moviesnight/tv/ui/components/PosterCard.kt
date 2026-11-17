package com.moviesnight.tv.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.moviesnight.tv.data.model.RecordSummary
import com.moviesnight.tv.data.remote.TmdbImage
import com.moviesnight.tv.ui.theme.FocusGlow
import com.moviesnight.tv.ui.theme.Surface
import com.moviesnight.tv.ui.theme.TextPrimary
import com.moviesnight.tv.ui.theme.TextSecondary
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.moviesnight.tv.ui.preview.PreviewSurface
import com.moviesnight.tv.ui.preview.SampleRecords
import com.moviesnight.tv.ui.preview.TvComponentPreview

private val PosterShape = RoundedCornerShape(6.dp)
const val POSTER_ASPECT = 2f / 3f
private const val FOCUSED_SCALE = 1.08f

/**
 * Poster + stars. Title and year fade in over the poster only while focused,
 * so the grid stays quiet and nothing shifts when focus moves.
 */
@Composable
fun PosterCard(
    record: RecordSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** Off where the tier is already clear from the surroundings (the tier list). */
    showTier: Boolean = true,
) {
    var focused by remember { mutableStateOf(false) }
    val captionAlpha by animateFloatAsState(if (focused) 1f else 0f, label = "caption")

    val scale by animateFloatAsState(if (focused) FOCUSED_SCALE else 1f, label = "scale")

    // Focused card draws above its neighbours, so the scaled poster and glow aren't covered.
    Column(modifier.zIndex(if (focused) 1f else 0f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // The poster scales here rather than inside Card, so the stickers scale with it.
        // Scaling at draw time (not with graphicsLayer) keeps layout bounds unchanged; otherwise
        // the grid's bring-into-view sees the growing poster and the page wobbles on focus moves.
        Box(Modifier.drawWithContent { scale(scale) { this@drawWithContent.drawContent() } }) {
            Card(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(POSTER_ASPECT)
                    .onFocusChanged { focused = it.hasFocus || it.isFocused },
                shape = CardDefaults.shape(PosterShape),
                colors = CardDefaults.colors(containerColor = Surface, focusedContainerColor = Surface),
                scale = CardDefaults.scale(focusedScale = 1f),
                border = CardDefaults.border(
                    focusedBorder = Border(BorderStroke(2.dp, Color.White.copy(alpha = 0.85f)), shape = PosterShape),
                ),
                glow = CardDefaults.glow(focusedGlow = Glow(elevationColor = FocusGlow, elevation = 18.dp)),
            ) {
                Box(Modifier.fillMaxSize()) {
                    if (record.posterPath != null) {
                        AsyncImage(
                            model = TmdbImage.gridPoster(record.posterPath),
                            contentDescription = record.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        TitleTile(record.title)
                    }
                    Caption(
                        record = record,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .alpha(captionAlpha),
                    )
                }
            }
            if (showTier) TierBadge(
                tier = record.tier,
                size = TierBadgeSize.Small,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    // tv Card raises its own z-index while focused; stay above it.
                    .zIndex(2f)
                    .offset(TierBadgeSize.Small.offsetX, TierBadgeSize.Small.offsetY),
            )
            if (record.highlyRecommended) {
                RecommendedBadge(
                    size = RecommendedBadgeSize.Small,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .zIndex(2f)
                        .offset(RecommendedBadgeSize.Small.offsetX, RecommendedBadgeSize.Small.offsetY),
                )
            }
        }
        // Stars on the left, review icon pinned to the card's right edge.
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StarRating(record.rating)
            if (record.hasReview) ReviewIndicator()
        }
    }
}

@Composable
private fun Caption(record: RecordSummary, modifier: Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))))
            .padding(start = 10.dp, end = 10.dp, top = 28.dp, bottom = 10.dp),
    ) {
        Text(
            record.title,
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(record.year.toString(), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
    }
}

/** Shown when a record has no poster. */
@Composable
private fun TitleTile(title: String) {
    Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * First card focused (with tier), then unfocused cards: with poster, no tier, and no poster.
 * Static previews may not finish the focus animation; use Interactive mode to move focus around.
 */
@TvComponentPreview
@Composable
private fun PosterCardPreview() {
    val samples = SampleRecords.summaries
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocusWhenReady() }
    PreviewSurface {
        Row(Modifier.padding(32.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            listOf(samples[0], samples[2], samples[1], samples[6]).forEachIndexed { i, record ->
                PosterCard(
                    record = record,
                    onClick = {},
                    modifier = Modifier
                        .width(140.dp)
                        .then(if (i == 0) Modifier.focusRequester(focus) else Modifier),
                )
            }
        }
    }
}
