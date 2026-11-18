package com.moviesnight.tv.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.moviesnight.tv.data.model.RecordSummary
import com.moviesnight.tv.data.model.TierGroup
import com.moviesnight.tv.ui.components.CenteredMessage
import com.moviesnight.tv.ui.components.EdgePadding
import com.moviesnight.tv.ui.components.ErrorState
import com.moviesnight.tv.ui.components.LoadingGrid
import com.moviesnight.tv.ui.components.NoAutoScroll
import com.moviesnight.tv.ui.components.PosterCard
import com.moviesnight.tv.ui.components.requestFocusWhenReady
import com.moviesnight.tv.ui.components.scrollItemToPivot
import com.moviesnight.tv.ui.components.tierColor
import com.moviesnight.tv.ui.preview.PreviewSurface
import com.moviesnight.tv.ui.preview.SampleRecords
import com.moviesnight.tv.ui.preview.TvScreenPreview
import com.moviesnight.tv.ui.theme.TextSecondary
import kotlinx.coroutines.launch

private const val COLUMNS = 6
private val LabelWidth = 64.dp
private val LabelHeight = 108.dp
/** Space between the tier label column and the first poster. */
private val LabelGap = 24.dp
private val PosterGap = 20.dp
private val RowGap = 28.dp
/** Extra space below a tier's last row, before the next tier starts. */
private val TierGap = 36.dp
private val TopPadding = 20.dp
/** Where a focused row rests below the top of the list, same as the poster grid. */
private val RowPivot = 140.dp
private val LabelShape = RoundedCornerShape(10.dp)
/** Ungraded anime ("?"), as the website's muted row. */
private val UnrankedColor = Color(0xFF3A3A3A)

/** One line of posters; a tier with more anime than [COLUMNS] spans several. */
private class TierRow(
    val group: Int,
    val records: List<RecordSummary>,
    val isFirst: Boolean,
    val isLast: Boolean,
)

/**
 * The anime tier list: each tier's posters wrap to the right of a colored label column.
 * The label (letter + count) sticks to the top while scrolling through a long tier, and a
 * solid band in the tier's color runs down beside all of its rows.
 *
 * Focus works like [com.moviesnight.tv.ui.components.RecordsGrid]: [takeFocus] focuses
 * [focusTargetId] if it's in the list, otherwise the first visible poster.
 */
@Composable
fun AnimeTierList(
    state: TierListState,
    listState: LazyListState,
    takeFocus: Boolean,
    focusTargetId: Long?,
    onFocusTaken: () -> Unit,
    onRecordFocused: (Long) -> Unit,
    onOpenRecord: (Long) -> Unit,
    onRetry: () -> Unit,
    topInset: Dp = 0.dp,
    contentFocus: FocusRequester? = null,
) {
    when (state) {
        TierListState.Loading -> LoadingGrid(topInset)
        TierListState.Error -> ErrorState("Couldn't load the tier list.", onRetry, takeFocus, topInset)
        is TierListState.Loaded ->
            if (state.groups.isEmpty()) {
                CenteredMessage("No anime in the tier list yet.", topInset)
            } else {
                TierRows(state.groups, listState, takeFocus, focusTargetId, onFocusTaken, onRecordFocused, onOpenRecord, topInset, contentFocus)
            }
    }
}

@OptIn(ExperimentalFoundationApi::class) // LocalBringIntoViewSpec
@Composable
private fun TierRows(
    groups: List<TierGroup>,
    listState: LazyListState,
    takeFocus: Boolean,
    focusTargetId: Long?,
    onFocusTaken: () -> Unit,
    onRecordFocused: (Long) -> Unit,
    onOpenRecord: (Long) -> Unit,
    topInset: Dp,
    contentFocus: FocusRequester?,
) {
    val rows = remember(groups) {
        groups.flatMapIndexed { g, group ->
            val chunks = group.records.chunked(COLUMNS)
            chunks.mapIndexed { i, chunk -> TierRow(g, chunk, isFirst = i == 0, isLast = i == chunks.lastIndex) }
        }
    }
    // Focus the remembered poster if it's in the list, else the first one on screen.
    val targetFocus = remember { FocusRequester() }
    val target = remember(rows) {
        rows.withIndex().firstNotNullOfOrNull { (r, row) ->
            row.records.indexOfFirst { it.id == focusTargetId }.takeIf { it >= 0 }?.let { r to it }
        } ?: (listState.firstVisibleItemIndex.coerceIn(rows.indices) to 0)
    }
    if (takeFocus) {
        LaunchedEffect(Unit) {
            targetFocus.requestFocusWhenReady()
            onFocusTaken()
        }
    }
    val pivotPx = with(LocalDensity.current) { RowPivot.toPx() }
    val scrollScope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        // The list scrolls itself (see scrollItemToPivot) for the same glide as the poster grid.
        CompositionLocalProvider(LocalBringIntoViewSpec provides NoAutoScroll) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(start = EdgePadding, end = EdgePadding, top = topInset + TopPadding, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(RowGap),
                // Coming back down from the header returns to the last focused poster.
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (contentFocus != null) Modifier.focusRequester(contentFocus) else Modifier)
                    .focusRestorer(),
            ) {
                itemsIndexed(rows, key = { _, row -> row.records.first().id }) { r, row ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = LabelWidth + LabelGap, bottom = if (row.isLast) TierGap else 0.dp),
                        horizontalArrangement = Arrangement.spacedBy(PosterGap),
                    ) {
                        for (c in 0 until COLUMNS) {
                            val record = row.records.getOrNull(c)
                            if (record == null) {
                                Spacer(Modifier.weight(1f))
                                continue
                            }
                            PosterCard(
                                record = record,
                                onClick = { onOpenRecord(record.id) },
                                showTier = false,
                                modifier = Modifier
                                    .weight(1f)
                                    .then(if (target == r to c) Modifier.focusRequester(targetFocus) else Modifier)
                                    .onFocusChanged {
                                        if (it.hasFocus) {
                                            onRecordFocused(record.id)
                                            scrollScope.launch { listState.scrollItemToPivot(r, pivotPx) }
                                        }
                                    },
                            )
                        }
                    }
                }
            }
        }
        TierLabels(groups, rows, listState, topInset)
    }
}

/**
 * Where tier [group] sits on screen, in px from the top of the list's viewport, or null
 * when none of its rows are visible. The ends are infinite when they're off screen.
 */
private fun LazyListLayoutInfo.tierSpan(group: Int, rows: List<TierRow>, tierGapPx: Float): ClosedFloatingPointRange<Float>? {
    val items = visibleItemsInfo.filter { rows.getOrNull(it.index)?.group == group }
    if (items.isEmpty()) return null
    val shift = -viewportStartOffset // item offsets don't include the top content padding
    val first = items.first()
    val last = items.last()
    val top = if (rows[first.index].isFirst) (first.offset + shift).toFloat() else Float.NEGATIVE_INFINITY
    val bottom = if (rows[last.index].isLast) last.offset + last.size + shift - tierGapPx else Float.POSITIVE_INFINITY
    return top..bottom
}

/** The label column: a solid band per tier in its color with its sticky letter tile on top. */
@Composable
private fun TierLabels(groups: List<TierGroup>, rows: List<TierRow>, listState: LazyListState, topInset: Dp) {
    val density = LocalDensity.current
    val tierGapPx = with(density) { TierGap.toPx() }
    // Labels stick just below the header, which the bands scroll under.
    val stickyTopPx = with(density) { (topInset + TopPadding).toPx() }
    val labelHeightPx = with(density) { LabelHeight.toPx() }
    val cornerPx = with(density) { 10.dp.toPx() }

    Box(
        Modifier
            .padding(start = EdgePadding)
            .width(LabelWidth)
            .fillMaxHeight()
            .clipToBounds(),
    ) {
        // Read in the draw phase, so scrolling redraws the bands without recomposing.
        Canvas(Modifier.fillMaxSize()) {
            val info = listState.layoutInfo
            groups.forEachIndexed { g, group ->
                val span = info.tierSpan(g, rows, tierGapPx) ?: return@forEachIndexed
                // Off-screen ends reach past the edge so their rounded corners stay hidden.
                val top = span.start.coerceAtLeast(-cornerPx)
                val bottom = span.endInclusive.coerceAtMost(size.height + cornerPx)
                if (bottom <= top) return@forEachIndexed
                drawRoundRect(
                    color = tierColor(group.tier) ?: UnrankedColor,
                    topLeft = Offset(0f, top),
                    size = Size(size.width, bottom - top),
                    cornerRadius = CornerRadius(cornerPx),
                )
            }
        }
        groups.forEachIndexed { g, group ->
            TierLabel(
                group = group,
                modifier = Modifier
                    // Sticks to the top while its tier scrolls by, then leaves with the tier's last row.
                    .offset {
                        val span = listState.layoutInfo.tierSpan(g, rows, tierGapPx)
                        val y = span?.let {
                            maxOf(it.start, stickyTopPx).coerceAtMost(it.endInclusive - labelHeightPx)
                        } ?: 0f
                        IntOffset(0, y.toInt())
                    }
                    .graphicsLayer { alpha = if (listState.layoutInfo.tierSpan(g, rows, tierGapPx) == null) 0f else 1f },
            )
        }
    }
}

/** Solid tile in the tier's color: the letter, "TIER" under it, and how many anime it holds. */
@Composable
private fun TierLabel(group: TierGroup, modifier: Modifier = Modifier) {
    val color = tierColor(group.tier)
    val glow = (color ?: Color.Black).copy(alpha = 0.5f)
    Column(
        modifier
            .size(LabelWidth, LabelHeight)
            .shadow(14.dp, LabelShape, ambientColor = glow, spotColor = glow)
            .background(color ?: UnrankedColor, LabelShape),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val content = if (color != null) Color.White else TextSecondary
        Text(
            group.tier ?: "?",
            style = MaterialTheme.typography.displaySmall.copy(fontSize = 38.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold),
            color = content,
        )
        Text(
            "TIER",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold),
            color = content.copy(alpha = 0.9f),
        )
        // Thin rule between "TIER" and the count.
        Box(
            Modifier
                .padding(vertical = 4.dp)
                .size(width = 24.dp, height = 1.dp)
                .background(content.copy(alpha = 0.6f)),
        )
        Text(
            group.records.size.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = content.copy(alpha = 0.8f),
        )
    }
}

@TvScreenPreview
@Composable
private fun AnimeTierListPreview() {
    PreviewSurface {
        AnimeTierList(
            state = TierListState.Loaded(SampleRecords.tierGroups),
            listState = remember { LazyListState() },
            takeFocus = true,
            focusTargetId = null,
            onFocusTaken = {},
            onRecordFocused = {},
            onOpenRecord = {},
            onRetry = {},
        )
    }
}
