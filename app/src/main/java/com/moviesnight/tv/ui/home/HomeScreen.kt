package com.moviesnight.tv.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
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
        onSelectCategory = viewModel::selectCategory,
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
    onSelectCategory: (HomeCategory) -> Unit,
    onSelect: (MediaFilter) -> Unit,
    records: (MediaFilter) -> Flow<PagingData<RecordSummary>>,
    tierList: TierListState,
    onRetryTierList: () -> Unit,
    onOpenRecord: (Long) -> Unit,
    onOpenSearch: () -> Unit,
) {
    val category = selected.category
    // Saveable, so scroll positions survive a trip to the detail screen and back.
    val gridStates = MediaFilter.entries.associateWith {
        rememberSaveable(it.name, saver = LazyGridState.Saver) { LazyGridState() }
    }
    val tierListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val categoryFocus = remember { HomeCategory.entries.associateWith { FocusRequester() } }
    val viewFocus = remember { MediaFilter.entries.associateWith { FocusRequester() } }
    var topRowFocused by remember { mutableStateOf(false) }
    val contentFocus = remember { FocusRequester() }
    // Last focused poster; focus returns to it when coming back from the detail screen.
    var focusedId by rememberSaveable { mutableStateOf<Long?>(null) }
    // True each time this screen (re)enters composition: at launch and on return from detail.
    var needsFocus by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    var headerFocused by remember { mutableStateOf(false) }
    // The view tabs hide while scrolling through records, and come back at the top
    // or whenever the header has focus.
    val atTop by remember(selected) {
        derivedStateOf {
            if (selected == MediaFilter.AnimeTiers) {
                tierListState.firstVisibleItemIndex == 0 && tierListState.firstVisibleItemScrollOffset == 0
            } else {
                val grid = gridStates.getValue(selected)
                grid.firstVisibleItemIndex == 0 && grid.firstVisibleItemScrollOffset == 0
            }
        }
    }
    val showViews = headerFocused || atTop
    // The header floats over the content, which starts below it and scrolls up underneath.
    // The inset always counts the view tabs, so the grid doesn't shift as they hide and show.
    // Saveable, so the grid doesn't shift for a frame when coming back from the detail screen.
    var topRowHeightPx by rememberSaveable { mutableStateOf(0) }
    val headerHeight = with(LocalDensity.current) { topRowHeightPx.toDp() } + HeaderTop + ViewRowHeight + HeaderBottom

    // Back from the grid or the view tabs: jump to the top and the category tabs.
    // Back on the category tabs (or search): leave the app.
    BackHandler(enabled = !topRowFocused) {
        scope.launch {
            if (selected == MediaFilter.AnimeTiers) tierListState.scrollToItem(0)
            else gridStates.getValue(selected).scrollToItem(0)
        }
        runCatching { categoryFocus.getValue(category).requestFocus() }
    }

    Box(
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
        // Up out of the content goes to the current view tab (or category tab). By geometry it
        // would pick whatever header item is nearest, usually the search button on the left.
        Box(
            Modifier
                .fillMaxSize()
                .focusProperties {
                    onExit = {
                        if (requestedFocusDirection == FocusDirection.Up) {
                            val target = if (category.views.size > 1) viewFocus.getValue(selected)
                            else categoryFocus.getValue(category)
                            if (runCatching { target.requestFocus(FocusDirection.Up) }.getOrDefault(false)) {
                                cancelFocusChange()
                            }
                        }
                    }
                }
                .focusGroup(),
        ) {
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
                    topInset = headerHeight,
                    contentFocus = contentFocus,
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
                    topInset = headerHeight,
                    contentFocus = contentFocus,
                )
            }
        }

        // Header, drawn last so it sits on top of the content: website logo on the left,
        // search + category tabs on the right, and the category's view tabs below them.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { headerFocused = it.hasFocus }
                // The grid now spans the whole screen behind the header, so Down from the header's
                // bottom row finds nothing below by geometry. Send it to the content explicitly.
                .onPreviewKeyEvent {
                    val onBottomRow = !topRowFocused || category.views.size <= 1 || !showViews
                    if (it.key == Key.DirectionDown && it.type == KeyEventType.KeyDown && onBottomRow) {
                        // Not attached while loading or on an error; then normal focus search runs.
                        runCatching { contentFocus.requestFocus(FocusDirection.Down) }.getOrDefault(false)
                    } else {
                        false
                    }
                }
                // Translucent, so posters stay faintly visible as they scroll underneath.
                .background(HeaderScrim)
                // Tabs carry TabPadding inside, so pull them out to line up with the grid edge.
                .padding(start = EdgePadding, end = EdgePadding - TabPadding, top = HeaderTop, bottom = HeaderBottom),
            horizontalAlignment = Alignment.End,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().onSizeChanged { topRowHeightPx = it.height },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Logo()
                Row(
                    modifier = Modifier.onFocusChanged { topRowFocused = it.hasFocus },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SearchButton(onClick = onOpenSearch)
                    HeaderTabs(
                        items = HomeCategory.entries,
                        selected = category,
                        onSelect = onSelectCategory,
                        label = { it.label },
                        focusRequesters = categoryFocus,
                        textStyle = MaterialTheme.typography.titleMedium,
                        verticalPadding = 8.dp,
                    )
                }
            }
            AnimatedVisibility(
                visible = showViews,
                enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
            ) {
                // Always the same height, so the grid doesn't jump while moving across categories.
                Box(Modifier.height(ViewRowHeight), contentAlignment = Alignment.CenterEnd) {
                    if (category.views.size > 1) key(category) {
                        HeaderTabs(
                            items = category.views,
                            selected = selected,
                            onSelect = onSelect,
                            label = { it.viewLabel },
                            focusRequesters = viewFocus,
                            textStyle = MaterialTheme.typography.titleSmall,
                            verticalPadding = 4.dp,
                        )
                    }
                }
            }
        }
    }
}

private val TabPadding = 8.dp

/** Mostly opaque at the top, lighter toward the bottom edge where the posters pass under. */
private val HeaderScrim = Brush.verticalGradient(
    0f to Background.copy(alpha = 0.97f),
    1f to Background.copy(alpha = 0.88f),
)
private val ViewRowHeight = 40.dp
private val HeaderTop = 24.dp
private val HeaderBottom = 4.dp

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

/** A row of tabs that switch on focus: the categories, or the current category's views. */
@Composable
private fun <T> HeaderTabs(
    items: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    focusRequesters: Map<T, FocusRequester>,
    textStyle: TextStyle,
    verticalPadding: Dp,
    modifier: Modifier = Modifier,
) {
    val selectedIndex = items.indexOf(selected)
    TabRow(
        selectedTabIndex = selectedIndex,
        // Coming in from above or below lands on the current tab instead of the nearest one.
        modifier = modifier.focusRestorer(focusRequesters.getValue(selected)),
        // Tab padding alone spaces the labels.
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
        items.forEach { item ->
            Tab(
                selected = item == selected,
                onFocus = { onSelect(item) },
                modifier = Modifier.focusRequester(focusRequesters.getValue(item)),
                colors = TabDefaults.underlinedIndicatorTabColors(
                    contentColor = TextSecondary,
                    selectedContentColor = TextPrimary,
                    focusedContentColor = TextPrimary,
                    focusedSelectedContentColor = TextPrimary,
                ),
            ) {
                Text(
                    label(item),
                    style = textStyle,
                    modifier = Modifier.padding(horizontal = TabPadding, vertical = verticalPadding),
                )
            }
        }
    }
}

/** Tabs switch in Interactive mode; the other grids show a smaller sample list, Anime › Tiers the tier list. */
@TvScreenPreview
@Composable
private fun HomePreview() {
    var selected by remember { mutableStateOf(MediaFilter.All) }
    PreviewSurface {
        HomeContent(
            selected = selected,
            onSelectCategory = { selected = it.views.first() },
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
            onSelectCategory = {},
            onSelect = {},
            records = { SampleRecords.pagedState(refresh) },
            tierList = TierListState.Loading,
            onRetryTierList = {},
            onOpenRecord = {},
            onOpenSearch = {},
        )
    }
}
