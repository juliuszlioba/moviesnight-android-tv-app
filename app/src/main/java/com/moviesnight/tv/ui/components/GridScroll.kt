package com.moviesnight.tv.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import kotlin.math.abs
import kotlin.math.max

/**
 * Turns off the grid's built-in "scroll focused item into view". Its motion can't be
 * customised (it jumps hard at the start and creeps at the end), so the grid scrolls
 * itself with [scrollRowToPivot] instead.
 */
@OptIn(ExperimentalFoundationApi::class)
object NoAutoScroll : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float = 0f
}

/** Smooth ease-in-out, one even glide per row. */
private val RowScroll = tween<Float>(durationMillis = 300, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f))

/**
 * Scrolls so the row containing [index] rests [pivotPx] from the top of the grid:
 * a sliver of the row above stays visible and the next row peeks in below, so the
 * D-pad always has a composed row to move to. Moving within a row scrolls nothing.
 *
 * When Down is pressed faster than the glide, focus can reach a row that isn't on screen
 * yet (the previous glide was cancelled mid-way). Its position is then worked out from
 * the nearest visible row, so the grid still follows instead of leaving focus off screen.
 */
suspend fun LazyGridState.scrollRowToPivot(index: Int, columns: Int, pivotPx: Float) {
    val info = layoutInfo
    val visible = info.visibleItemsInfo.filter { it.index >= 0 }
    val anchor = visible.firstOrNull { it.index == index }
        ?: visible.minByOrNull { abs(it.index / columns - index / columns) }
        ?: return
    val rowStride = anchor.size.height + info.mainAxisItemSpacing
    val rowsAway = index / columns - anchor.index / columns
    var delta = anchor.offset.y + rowsAway * rowStride - pivotPx
    if (index < columns) {
        // First row: settle at the very top instead of running into the scroll limit.
        delta = max(delta, -firstVisibleItemScrollOffset.toFloat())
    }
    if (abs(delta) >= 1f) animateScrollBy(delta, RowScroll)
}

/**
 * [scrollRowToPivot] for a list whose items are whole rows (the tier list): scrolls so
 * item [index] rests [pivotPx] from the top of the list.
 */
suspend fun LazyListState.scrollItemToPivot(index: Int, pivotPx: Float) {
    val info = layoutInfo
    val visible = info.visibleItemsInfo
    val anchor = visible.firstOrNull { it.index == index }
        ?: visible.minByOrNull { abs(it.index - index) }
        ?: return
    val stride = anchor.size + info.mainAxisItemSpacing
    var delta = anchor.offset + (index - anchor.index) * stride - pivotPx
    if (index == 0) {
        // First row: settle at the very top instead of running into the scroll limit.
        delta = max(delta, -firstVisibleItemScrollOffset.toFloat())
    }
    if (abs(delta) >= 1f) animateScrollBy(delta, RowScroll)
}
