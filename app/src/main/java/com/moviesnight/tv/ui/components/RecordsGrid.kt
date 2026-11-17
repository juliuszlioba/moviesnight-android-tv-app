package com.moviesnight.tv.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.moviesnight.tv.data.model.RecordSummary
import com.moviesnight.tv.ui.theme.Surface
import com.moviesnight.tv.ui.theme.TextPrimary
import com.moviesnight.tv.ui.theme.TextSecondary
import kotlinx.coroutines.launch

private const val COLUMNS = 6
/** TV overscan-safe side margin, shared by the grid and the headers above it. */
val EdgePadding = 48.dp
private val GridGap = 20.dp
/** Where a focused row rests below the top of the grid (shows a sliver of the row above). */
private val RowPivot = 140.dp

/**
 * The paged poster grid used by the home and search screens, including its
 * loading, error and empty states.
 *
 * @param takeFocus focus a poster as soon as one is shown: [focusTargetId] if it's
 *   loaded, otherwise the first visible one. [onFocusTaken] is called once it has focus.
 */
@OptIn(ExperimentalFoundationApi::class) // LocalBringIntoViewSpec
@Composable
fun RecordsGrid(
    items: LazyPagingItems<RecordSummary>,
    state: LazyGridState,
    takeFocus: Boolean,
    focusTargetId: Long?,
    onFocusTaken: () -> Unit,
    onRecordFocused: (Long) -> Unit,
    onOpenRecord: (Long) -> Unit,
    emptyMessage: String = "Nothing here yet.",
    errorMessage: String = "Couldn't load your list.",
) {
    val refresh = items.loadState.refresh
    when {
        items.itemCount == 0 && refresh is LoadState.Loading -> LoadingGrid()
        items.itemCount == 0 && refresh is LoadState.Error -> ErrorState(
            message = errorMessage,
            onRetry = items::retry,
            takeFocus = takeFocus,
        )
        items.itemCount == 0 && refresh is LoadState.NotLoading -> CenteredMessage(emptyMessage)
        else -> {
            // Focus the remembered poster if it's on screen, else the first visible one.
            val targetFocus = remember { FocusRequester() }
            val targetIndex = remember(items.itemCount) {
                val visible = state.firstVisibleItemIndex
                (0 until items.itemCount).firstOrNull { items.peek(it)?.id == focusTargetId } ?: visible
            }
            if (takeFocus) {
                LaunchedEffect(Unit) {
                    targetFocus.requestFocusWhenReady()
                    onFocusTaken()
                }
            }
            val pivotPx = with(LocalDensity.current) { RowPivot.toPx() }
            val scrollScope = rememberCoroutineScope()
            // The grid scrolls itself (see scrollRowToPivot) for a smooth, even glide per row.
            CompositionLocalProvider(LocalBringIntoViewSpec provides NoAutoScroll) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(COLUMNS),
                    state = state,
                    contentPadding = PaddingValues(start = EdgePadding, end = EdgePadding, top = 20.dp, bottom = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(GridGap),
                    verticalArrangement = Arrangement.spacedBy(28.dp),
                    // Coming back down from the header returns to the last focused poster.
                    modifier = Modifier.fillMaxSize().focusRestorer(),
                ) {
                    items(count = items.itemCount, key = items.itemKey { it.id }) { index ->
                        val record = items[index] ?: return@items
                        PosterCard(
                            record = record,
                            onClick = { onOpenRecord(record.id) },
                            modifier = Modifier
                                .then(if (index == targetIndex) Modifier.focusRequester(targetFocus) else Modifier)
                                .onFocusChanged {
                                    if (it.hasFocus) {
                                        onRecordFocused(record.id)
                                        scrollScope.launch { state.scrollRowToPivot(index, COLUMNS, pivotPx) }
                                    }
                                },
                        )
                    }
                    if (items.loadState.append is LoadState.Error) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                                Button(onClick = items::retry) { Text("Load more") }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Quiet pulsing poster placeholders while the first page loads. */
@Composable
fun LoadingGrid() {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.35f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse",
    )
    LazyVerticalGrid(
        columns = GridCells.Fixed(COLUMNS),
        contentPadding = PaddingValues(start = EdgePadding, end = EdgePadding, top = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(GridGap),
        verticalArrangement = Arrangement.spacedBy(28.dp),
        userScrollEnabled = false,
        modifier = Modifier.fillMaxSize().alpha(pulse),
    ) {
        items(COLUMNS * 2) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.fillMaxWidth().aspectRatio(POSTER_ASPECT)
                        .clip(RoundedCornerShape(6.dp)).background(Surface),
                )
                Box(Modifier.width(80.dp).height(14.dp).clip(RoundedCornerShape(3.dp)).background(Surface))
            }
        }
    }
}

/** Message plus a Retry button, which takes focus when [takeFocus] is set. */
@Composable
fun ErrorState(message: String, onRetry: () -> Unit, takeFocus: Boolean) {
    val retryFocus = remember { FocusRequester() }
    if (takeFocus) LaunchedEffect(Unit) { runCatching { retryFocus.requestFocus() } }
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Spacer(Modifier.height(6.dp))
        Text("Check the Shield's connection and try again.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry, modifier = Modifier.focusRequester(retryFocus)) { Text("Retry") }
    }
}

/** A quiet line of text centered in the space the grid would take. */
@Composable
fun CenteredMessage(text: String) {
    Box(Modifier.fillMaxSize().padding(horizontal = EdgePadding), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.titleMedium, color = TextSecondary, textAlign = TextAlign.Center)
    }
}
