package com.moviesnight.tv.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import com.moviesnight.tv.ui.components.NoAutoScroll
import com.moviesnight.tv.ui.components.scrollRowToPivot
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Tab
import androidx.tv.material3.TabDefaults
import androidx.tv.material3.TabRow
import androidx.tv.material3.TabRowDefaults
import androidx.tv.material3.Text
import com.moviesnight.tv.data.model.MediaFilter
import com.moviesnight.tv.data.model.RecordSummary
import com.moviesnight.tv.ui.components.POSTER_ASPECT
import com.moviesnight.tv.ui.components.PosterCard
import com.moviesnight.tv.ui.components.Logo
import com.moviesnight.tv.ui.components.requestFocusWhenReady
import com.moviesnight.tv.ui.theme.Surface
import com.moviesnight.tv.ui.theme.TextPrimary
import com.moviesnight.tv.ui.theme.TextSecondary
import kotlinx.coroutines.launch

private const val COLUMNS = 6
private val EdgePadding = 48.dp // TV overscan-safe side margin
private val GridGap = 20.dp
/** Where a focused row rests below the top of the grid (shows a sliver of the row above). */
private val RowPivot = 140.dp

@Composable
fun HomeScreen(
    onOpenRecord: (Long) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    // Saveable, so scroll positions survive a trip to the detail screen and back.
    val gridStates = MediaFilter.entries.associateWith {
        rememberSaveable(it.name, saver = LazyGridState.Saver) { LazyGridState() }
    }
    val tabFocus = remember { MediaFilter.entries.associateWith { FocusRequester() } }
    var tabsFocused by remember { mutableStateOf(false) }
    // Last focused poster; focus returns to it when coming back from the detail screen.
    var focusedId by rememberSaveable { mutableStateOf<Long?>(null) }
    // True each time this screen (re)enters composition: at launch and on return from detail.
    var needsFocus by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    // Back from the grid: jump to the top and the tabs. Back on the tabs: leave the app.
    BackHandler(enabled = !tabsFocused) {
        scope.launch { gridStates.getValue(selected).scrollToItem(0) }
        runCatching { tabFocus.getValue(selected).requestFocus() }
    }

    Column(Modifier.fillMaxSize()) {
        // Header: website logo on the left, tabs on the right.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // Tabs carry 12dp inner padding, so pull them out to line up with the grid edge.
                .padding(start = EdgePadding, end = EdgePadding - 12.dp, top = 24.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Logo()
            FilterTabs(
                selected = selected,
                onSelect = viewModel::select,
                focusRequesters = tabFocus,
                modifier = Modifier.onFocusChanged { tabsFocused = it.hasFocus },
            )
        }

        key(selected) {
            val items = viewModel.records.getValue(selected).collectAsLazyPagingItems()
            RecordsGrid(
                items = items,
                state = gridStates.getValue(selected),
                takeFocus = needsFocus,
                focusTargetId = focusedId,
                onFocusTaken = { needsFocus = false },
                onRecordFocused = { focusedId = it },
                onOpenRecord = onOpenRecord,
            )
        }
    }
}

@Composable
private fun FilterTabs(
    selected: MediaFilter,
    onSelect: (MediaFilter) -> Unit,
    focusRequesters: Map<MediaFilter, FocusRequester>,
    modifier: Modifier = Modifier,
) {
    val selectedIndex = selected.ordinal
    TabRow(
        selectedTabIndex = selectedIndex,
        // Coming up from the grid lands on the current tab instead of the nearest one.
        modifier = modifier.focusRestorer(focusRequesters.getValue(selected)),
        indicator = { positions, rowFocused ->
            positions.getOrNull(selectedIndex)?.let {
                TabRowDefaults.UnderlinedIndicator(
                    currentTabPosition = it,
                    doesTabRowHaveFocus = rowFocused,
                    activeColor = TextPrimary,
                    inactiveColor = TextSecondary,
                )
            }
        },
    ) {
        MediaFilter.entries.forEach { filter ->
            Tab(
                selected = filter == selected,
                onFocus = { onSelect(filter) },
                modifier = Modifier.focusRequester(focusRequesters.getValue(filter)),
                colors = TabDefaults.underlinedIndicatorTabColors(
                    contentColor = TextSecondary,
                    selectedContentColor = TextPrimary,
                    focusedContentColor = TextPrimary,
                    focusedSelectedContentColor = TextPrimary,
                ),
            ) {
                Text(
                    filter.label,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class) // LocalBringIntoViewSpec
@Composable
private fun RecordsGrid(
    items: LazyPagingItems<RecordSummary>,
    state: LazyGridState,
    takeFocus: Boolean,
    focusTargetId: Long?,
    onFocusTaken: () -> Unit,
    onRecordFocused: (Long) -> Unit,
    onOpenRecord: (Long) -> Unit,
) {
    val refresh = items.loadState.refresh
    when {
        items.itemCount == 0 && refresh is LoadState.Loading -> LoadingGrid()
        items.itemCount == 0 && refresh is LoadState.Error -> ErrorState(
            message = "Couldn't load your list.",
            onRetry = items::retry,
            takeFocus = takeFocus,
        )
        items.itemCount == 0 && refresh is LoadState.NotLoading -> CenteredMessage("Nothing here yet.")
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
                // Coming back down from the tabs returns to the last focused poster.
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
private fun LoadingGrid() {
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

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit, takeFocus: Boolean) {
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

@Composable
private fun CenteredMessage(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.titleMedium, color = TextSecondary)
    }
}
