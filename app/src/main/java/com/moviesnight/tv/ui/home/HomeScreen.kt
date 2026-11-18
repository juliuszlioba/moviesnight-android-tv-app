package com.moviesnight.tv.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.IconButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Tab
import androidx.tv.material3.TabDefaults
import androidx.tv.material3.TabRow
import androidx.tv.material3.TabRowDefaults
import androidx.tv.material3.Text
import com.moviesnight.tv.data.model.MediaFilter
import com.moviesnight.tv.data.model.RecordSummary
import com.moviesnight.tv.ui.components.EdgePadding
import com.moviesnight.tv.ui.components.Logo
import com.moviesnight.tv.ui.components.RecordsGrid
import com.moviesnight.tv.ui.components.SearchIcon
import com.moviesnight.tv.ui.preview.PreviewSurface
import com.moviesnight.tv.ui.preview.SampleRecords
import com.moviesnight.tv.ui.preview.TvScreenPreview
import com.moviesnight.tv.ui.theme.Background
import com.moviesnight.tv.ui.theme.TextPrimary
import com.moviesnight.tv.ui.theme.TextSecondary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onOpenRecord: (Long) -> Unit,
    onOpenSearch: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val tierList by viewModel.tierList.collectAsStateWithLifecycle()
    HomeContent(
        selected = selected,
        onSelect = viewModel::select,
        records = viewModel.records::getValue,
        tierList = tierList,
        onRetryTierList = viewModel::retryTierList,
        onOpenRecord = onOpenRecord,
        onOpenSearch = onOpenSearch,
    )
}

/** The home screen without its ViewModel, so previews can feed it sample data. */
@Composable
fun HomeContent(
    selected: MediaFilter,
    onSelect: (MediaFilter) -> Unit,
    records: (MediaFilter) -> Flow<PagingData<RecordSummary>>,
    tierList: TierListState,
    onRetryTierList: () -> Unit,
    onOpenRecord: (Long) -> Unit,
    onOpenSearch: () -> Unit,
) {
    // Saveable, so scroll positions survive a trip to the detail screen and back.
    val gridStates = MediaFilter.entries.associateWith {
        rememberSaveable(it.name, saver = LazyGridState.Saver) { LazyGridState() }
    }
    val tierListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val tabFocus = remember { MediaFilter.entries.associateWith { FocusRequester() } }
    var headerFocused by remember { mutableStateOf(false) }
    // Last focused poster; focus returns to it when coming back from the detail screen.
    var focusedId by rememberSaveable { mutableStateOf<Long?>(null) }
    // True each time this screen (re)enters composition: at launch and on return from detail.
    var needsFocus by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    // Back from the grid: jump to the top and the tabs. Back in the header: leave the app.
    BackHandler(enabled = !headerFocused) {
        scope.launch {
            if (selected == MediaFilter.AnimeTiers) tierListState.scrollToItem(0)
            else gridStates.getValue(selected).scrollToItem(0)
        }
        runCatching { tabFocus.getValue(selected).requestFocus() }
    }

    Column(
        Modifier
            .fillMaxSize()
            // The Search key on remotes/keyboards that have one opens search from anywhere.
            .onPreviewKeyEvent {
                if (it.key == Key.Search && it.type == KeyEventType.KeyUp) {
                    onOpenSearch()
                    true
                } else {
                    false
                }
            },
    ) {
        // Header: website logo on the left, search + tabs on the right.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // Tabs carry TabPadding inside, so pull them out to line up with the grid edge.
                .padding(start = EdgePadding, end = EdgePadding - TabPadding, top = 24.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Logo()
            Row(
                modifier = Modifier.onFocusChanged { headerFocused = it.hasFocus },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SearchButton(onClick = onOpenSearch)
                FilterTabs(
                    selected = selected,
                    onSelect = onSelect,
                    focusRequesters = tabFocus,
                )
            }
        }

        if (selected == MediaFilter.AnimeTiers) {
            AnimeTierList(
                state = tierList,
                listState = tierListState,
                takeFocus = needsFocus,
                focusTargetId = focusedId,
                onFocusTaken = { needsFocus = false },
                onRecordFocused = { focusedId = it },
                onOpenRecord = onOpenRecord,
                onRetry = onRetryTierList,
            )
        } else key(selected) {
            val items = remember(selected) { records(selected) }.collectAsLazyPagingItems()
            RecordsGrid(
                items = items,
                state = gridStates.getValue(selected),
                takeFocus = needsFocus,
                focusTargetId = focusedId,
                onFocusTaken = { needsFocus = false },
                onRecordFocused = { focusedId = it },
                onOpenRecord = onOpenRecord,
                // Being on these lists is what makes a record highly recommended.
                showRecommended = selected != MediaFilter.TopMovies && selected != MediaFilter.TopSeries,
            )
        }
    }
}

private val TabPadding = 8.dp

/** Magnifier left of the tabs; Left from the first tab reaches it. */
@Composable
private fun SearchButton(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp),
        scale = IconButtonDefaults.scale(focusedScale = 1f),
        colors = IconButtonDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = TextSecondary,
            focusedContainerColor = TextPrimary,
            focusedContentColor = Background,
        ),
    ) {
        Icon(SearchIcon, contentDescription = "Search", modifier = Modifier.size(22.dp))
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
        // Tab padding alone spaces the labels, so all eight fit in the header.
        separator = { Spacer(Modifier.width(0.dp)) },
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
                    modifier = Modifier.padding(horizontal = TabPadding, vertical = 8.dp),
                )
            }
        }
    }
}

/** Tabs switch in Interactive mode; the other grid tabs show a smaller sample list, the last tab the tier list. */
@TvScreenPreview
@Composable
private fun HomePreview() {
    var selected by remember { mutableStateOf(MediaFilter.All) }
    PreviewSurface {
        HomeContent(
            selected = selected,
            onSelect = { selected = it },
            records = { filter ->
                if (filter == MediaFilter.All) SampleRecords.paged()
                else SampleRecords.paged(SampleRecords.summaries.filterIndexed { i, _ -> i % 3 == filter.ordinal % 3 })
            },
            tierList = TierListState.Loaded(SampleRecords.tierGroups),
            onRetryTierList = {},
            onOpenRecord = {},
            onOpenSearch = {},
        )
    }
}

@TvScreenPreview
@Composable
private fun HomeLoadingPreview() = HomeStatePreview(LoadState.Loading)

@TvScreenPreview
@Composable
private fun HomeErrorPreview() = HomeStatePreview(LoadState.Error(Exception("offline")))

@TvScreenPreview
@Composable
private fun HomeEmptyPreview() = HomeStatePreview(LoadState.NotLoading(endOfPaginationReached = true))

@Composable
private fun HomeStatePreview(refresh: LoadState) {
    PreviewSurface {
        HomeContent(
            selected = MediaFilter.All,
            onSelect = {},
            records = { SampleRecords.pagedState(refresh) },
            tierList = TierListState.Loading,
            onRetryTierList = {},
            onOpenRecord = {},
            onOpenSearch = {},
        )
    }
}
