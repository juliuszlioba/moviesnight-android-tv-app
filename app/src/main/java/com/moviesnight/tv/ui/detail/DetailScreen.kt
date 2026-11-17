package com.moviesnight.tv.ui.detail

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.moviesnight.tv.data.model.MediaType
import com.moviesnight.tv.data.model.RecordDetail
import com.moviesnight.tv.data.remote.TmdbImage
import com.moviesnight.tv.ui.components.POSTER_ASPECT
import com.moviesnight.tv.ui.components.Quote
import com.moviesnight.tv.ui.components.StarRating
import com.moviesnight.tv.ui.components.TierBadge
import com.moviesnight.tv.ui.components.TierBadgeSize
import com.moviesnight.tv.ui.components.requestFocusWhenReady
import com.moviesnight.tv.ui.theme.Background
import com.moviesnight.tv.ui.theme.Star
import com.moviesnight.tv.ui.theme.Surface
import com.moviesnight.tv.ui.theme.TextPrimary
import com.moviesnight.tv.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.moviesnight.tv.ui.preview.PreviewSurface
import com.moviesnight.tv.ui.preview.SampleRecords
import com.moviesnight.tv.ui.preview.TvScreenPreview

private val PosterShape = RoundedCornerShape(8.dp)
/** Poster fills the screen height, so its top, left and bottom margins are all [Margin]. */
private val Margin = 48.dp
private val PosterSize = Modifier.fillMaxHeight().aspectRatio(POSTER_ASPECT, matchHeightConstraintsFirst = true)
private val ScreenPadding = Modifier.padding(Margin)

@Composable
fun DetailScreen(viewModel: DetailViewModel = viewModel(factory = DetailViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DetailContent(state, onRetry = viewModel::load)
}

/** The detail screen without its ViewModel, so previews can feed it sample data. */
@Composable
fun DetailContent(state: DetailUiState, onRetry: () -> Unit) {
    when (state) {
        DetailUiState.Loading -> LoadingDetail()
        DetailUiState.Error -> ErrorDetail(onRetry = onRetry)
        is DetailUiState.Loaded -> LoadedDetail(state.record)
    }
}

@Composable
private fun LoadedDetail(record: RecordDetail) {
    Row(
        modifier = Modifier.fillMaxSize().then(ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(48.dp),
    ) {
        Poster(record)
        ScrollableInfo(record, Modifier.weight(1f).fillMaxHeight())
    }
}

@Composable
private fun Poster(record: RecordDetail) {
    Box(PosterSize) {
        Box(
            Modifier.fillMaxSize().clip(PosterShape).background(Surface),
            contentAlignment = Alignment.Center,
        ) {
            if (record.posterPath != null) {
                AsyncImage(
                    model = TmdbImage.largePoster(record.posterPath),
                    contentDescription = record.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    record.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
        // Outside the clipped box so it can hang over the poster's left edge.
        TierBadge(
            tier = record.tier,
            size = TierBadgeSize.Large,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(TierBadgeSize.Large.offsetX, TierBadgeSize.Large.offsetY),
        )
    }
}

/**
 * The info column is the screen's only focus target. Up/Down scroll it, so long
 * reviews can be read with the remote; Back is left to navigation.
 */
@Composable
private fun ScrollableInfo(record: RecordDetail, modifier: Modifier) {
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    val focus = remember { FocusRequester() }
    val step = with(LocalDensity.current) { 140.dp.toPx() }
    val moreBelow by animateFloatAsState(if (scroll.canScrollForward) 1f else 0f, label = "moreBelow")
    val moreAbove by animateFloatAsState(if (scroll.canScrollBackward) 1f else 0f, label = "moreAbove")

    LaunchedEffect(Unit) { focus.requestFocusWhenReady() }

    Box(modifier) {
        Column(
            Modifier
                .fillMaxSize()
                .focusRequester(focus)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionDown -> { scope.launch { scroll.animateScrollBy(step) }; true }
                        Key.DirectionUp -> { scope.launch { scroll.animateScrollBy(-step) }; true }
                        else -> false
                    }
                }
                .verticalScroll(scroll)
                .padding(bottom = 32.dp),
        ) {
            Text(
                record.title,
                style = MaterialTheme.typography.displaySmall,
                color = TextPrimary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            Text(metaLine(record), style = MaterialTheme.typography.titleMedium, color = TextSecondary)

            Spacer(Modifier.height(24.dp))
            StarRating(record.rating, starSize = 26.dp, spacing = 6.dp)

            record.watchedAt?.let {
                Spacer(Modifier.height(16.dp))
                Text("Watched ${formatDate(it)}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
            if (record.genres.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(record.genres.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }

            record.recommendation?.let {
                Spacer(Modifier.height(32.dp))
                // Same look as the website: yellow text with a hanging yellow quote mark.
                Quote(
                    text = it,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal),
                    color = Star,
                )
            }
            record.review?.let {
                Spacer(Modifier.height(if (record.recommendation != null) 16.dp else 32.dp))
                Text(it, style = MaterialTheme.typography.bodyLarge, color = TextPrimary.copy(alpha = 0.85f))
            }
        }

        // Soft fades hint that the text continues.
        EdgeFade(Modifier.align(Alignment.TopCenter).alpha(moreAbove), fromTop = true)
        EdgeFade(Modifier.align(Alignment.BottomCenter).alpha(moreBelow), fromTop = false)
    }
}

@Composable
private fun EdgeFade(modifier: Modifier, fromTop: Boolean) {
    val colors = listOf(Background, Color.Transparent).let { if (fromTop) it else it.reversed() }
    Box(modifier.fillMaxWidth().height(48.dp).background(Brush.verticalGradient(colors)))
}

private fun metaLine(record: RecordDetail): String = listOfNotNull(
    record.year.toString(),
    if (record.mediaType == MediaType.Series) "Series" else "Movie",
    "Anime".takeIf { record.isAnime },
).joinToString("  ·  ")

private val DateFormat = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault())

private fun formatDate(instant: Instant): String =
    instant.atZone(ZoneId.systemDefault()).format(DateFormat)

@Composable
private fun LoadingDetail() {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.35f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse",
    )
    Row(
        Modifier.fillMaxSize().then(ScreenPadding).alpha(pulse),
        horizontalArrangement = Arrangement.spacedBy(48.dp),
    ) {
        Box(PosterSize.clip(PosterShape).background(Surface))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.width(320.dp).height(36.dp).clip(RoundedCornerShape(4.dp)).background(Surface))
            Box(Modifier.width(160.dp).height(16.dp).clip(RoundedCornerShape(4.dp)).background(Surface))
        }
    }
}

@Composable
private fun ErrorDetail(onRetry: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocusWhenReady() }
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Couldn't load this title.", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry, modifier = Modifier.focusRequester(focus)) { Text("Retry") }
    }
}

/** Long review: Up/Down scroll it in Interactive mode. */
@TvScreenPreview
@Composable
private fun DetailPreview() {
    PreviewSurface { DetailContent(DetailUiState.Loaded(SampleRecords.detail), onRetry = {}) }
}

/** No poster, tier, rating, date, genres or recommendation. */
@TvScreenPreview
@Composable
private fun DetailSparsePreview() {
    PreviewSurface { DetailContent(DetailUiState.Loaded(SampleRecords.sparseDetail), onRetry = {}) }
}

@TvScreenPreview
@Composable
private fun DetailLoadingPreview() {
    PreviewSurface { DetailContent(DetailUiState.Loading, onRetry = {}) }
}

@TvScreenPreview
@Composable
private fun DetailErrorPreview() {
    PreviewSurface { DetailContent(DetailUiState.Error, onRetry = {}) }
}
