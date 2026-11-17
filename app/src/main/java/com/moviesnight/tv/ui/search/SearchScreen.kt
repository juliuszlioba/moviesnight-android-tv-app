package com.moviesnight.tv.ui.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.moviesnight.tv.data.model.RecordSummary
import com.moviesnight.tv.ui.components.CenteredMessage
import com.moviesnight.tv.ui.components.EdgePadding
import com.moviesnight.tv.ui.components.RecordsGrid
import com.moviesnight.tv.ui.components.SearchIcon
import com.moviesnight.tv.ui.components.requestFocusWhenReady
import com.moviesnight.tv.ui.preview.PreviewSurface
import com.moviesnight.tv.ui.preview.SampleRecords
import com.moviesnight.tv.ui.preview.TvScreenPreview
import com.moviesnight.tv.ui.theme.Surface
import com.moviesnight.tv.ui.theme.TextPrimary
import com.moviesnight.tv.ui.theme.TextSecondary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

private val FieldShape = RoundedCornerShape(8.dp)

@Composable
fun SearchScreen(
    onOpenRecord: (Long) -> Unit,
    viewModel: SearchViewModel = viewModel(factory = SearchViewModel.Factory),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val activeQuery by viewModel.activeQuery.collectAsStateWithLifecycle()
    SearchContent(
        query = query,
        onQueryChange = viewModel::onQueryChange,
        onSubmit = viewModel::submit,
        activeQuery = activeQuery,
        results = viewModel::results,
        onOpenRecord = onOpenRecord,
    )
}

/** The search screen without its ViewModel, so previews can feed it sample data. */
@Composable
fun SearchContent(
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    activeQuery: String,
    results: (String) -> Flow<PagingData<RecordSummary>>,
    onOpenRecord: (Long) -> Unit,
) {
    val fieldFocus = remember { FocusRequester() }
    var fieldFocused by remember { mutableStateOf(false) }
    // Last focused poster; focus returns to it when coming back from the detail screen.
    var focusedId by rememberSaveable { mutableStateOf<Long?>(null) }
    // On entry, results get focus if the user was already in them, otherwise the field does.
    var gridNeedsFocus by remember { mutableStateOf(focusedId != null) }
    val keyboard = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    // A fresh scroll position per query; saveable so it survives a trip to the detail screen.
    val gridState = rememberSaveable(activeQuery, saver = LazyGridState.Saver) { LazyGridState() }

    LaunchedEffect(Unit) {
        if (!gridNeedsFocus) fieldFocus.requestFocusWhenReady()
    }

    // Back from the results: up to the field. Back in the field: leave search.
    BackHandler(enabled = !fieldFocused) {
        scope.launch { gridState.scrollToItem(0) }
        runCatching { fieldFocus.requestFocus() }
    }

    Column(Modifier.fillMaxSize()) {
        SearchField(
            value = query,
            onValueChange = {
                onQueryChange(it)
                focusedId = null
            },
            onSearch = {
                keyboard?.hide()
                onSubmit()
                // Jump into the results as soon as they're shown.
                gridNeedsFocus = true
            },
            modifier = Modifier
                .padding(start = EdgePadding, end = EdgePadding, top = 24.dp, bottom = 12.dp)
                .focusRequester(fieldFocus)
                .onFocusChanged { fieldFocused = it.hasFocus },
        )

        if (activeQuery.isEmpty()) {
            CenteredMessage("Search your list by title.")
        } else {
            key(activeQuery) {
                val items = remember(activeQuery) { results(activeQuery) }.collectAsLazyPagingItems()
                RecordsGrid(
                    items = items,
                    state = gridState,
                    takeFocus = gridNeedsFocus,
                    focusTargetId = focusedId,
                    onFocusTaken = { gridNeedsFocus = false },
                    onRecordFocused = { focusedId = it },
                    onOpenRecord = onOpenRecord,
                    emptyMessage = "Nothing matches “$activeQuery”.",
                    errorMessage = "Search failed.",
                )
            }
        }
    }
}

/**
 * Pill-shaped text field. OK opens the on-screen keyboard (which has voice input);
 * its search key, or D-pad Down, moves on to the results.
 */
@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.titleLarge.copy(color = TextPrimary),
        cursorBrush = SolidColor(TextPrimary),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            imeAction = ImeAction.Search,
        ),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        modifier = modifier
            .widthIn(max = 640.dp)
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused },
        decorationBox = { innerTextField ->
            Row(
                Modifier
                    .height(56.dp)
                    .background(Surface, FieldShape)
                    .border(2.dp, if (focused) Color.White.copy(alpha = 0.85f) else Color.Transparent, FieldShape)
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(
                    SearchIcon,
                    contentDescription = null,
                    tint = if (focused) TextPrimary else TextSecondary,
                    modifier = Modifier.size(24.dp),
                )
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text("Search by title", style = MaterialTheme.typography.titleLarge, color = TextSecondary)
                    }
                    innerTextField()
                }
            }
        },
    )
}

@TvScreenPreview
@Composable
private fun SearchPreview() {
    PreviewSurface {
        SearchContent(
            query = "the",
            onQueryChange = {},
            onSubmit = {},
            activeQuery = "the",
            results = { SampleRecords.paged(SampleRecords.summaries.filter { it.title.contains("the", ignoreCase = true) }) },
            onOpenRecord = {},
        )
    }
}

@TvScreenPreview
@Composable
private fun SearchEmptyPreview() {
    PreviewSurface {
        SearchContent(
            query = "",
            onQueryChange = {},
            onSubmit = {},
            activeQuery = "",
            results = { SampleRecords.paged(emptyList()) },
            onOpenRecord = {},
        )
    }
}
