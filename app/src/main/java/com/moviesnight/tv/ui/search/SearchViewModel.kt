package com.moviesnight.tv.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.moviesnight.tv.MoviesNightApp
import com.moviesnight.tv.data.RecordsRepository
import com.moviesnight.tv.data.model.RecordSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn

@OptIn(FlowPreview::class)
class SearchViewModel(private val repository: RecordsRepository) : ViewModel() {

    /** What's typed in the field, kept here so it survives a trip to the detail screen. */
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /** Set by the keyboard's search action to skip the typing pause. */
    private val submitted = MutableStateFlow("")

    /** The query results are shown for: trimmed, after a short pause in typing. */
    val activeQuery: StateFlow<String> =
        merge(_query.debounce(DEBOUNCE_MS), submitted)
            .map { it.trim() }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    private var cached: Pair<String, Flow<PagingData<RecordSummary>>>? = null
    private var cachedScope: CoroutineScope? = null

    /**
     * Results for [query], cached so returning from the detail screen keeps what was loaded.
     * Only the latest query is kept; the previous one's paging is cancelled.
     */
    fun results(query: String): Flow<PagingData<RecordSummary>> {
        cached?.let { (q, flow) -> if (q == query) return flow }
        cachedScope?.cancel()
        val scope = CoroutineScope(viewModelScope.coroutineContext + SupervisorJob(viewModelScope.coroutineContext[Job]))
        val flow = repository.searchRecords(query).cachedIn(scope)
        cachedScope = scope
        cached = query to flow
        return flow
    }

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun submit() {
        submitted.value = _query.value
    }

    companion object {
        private const val DEBOUNCE_MS = 350L

        val Factory = viewModelFactory {
            initializer { SearchViewModel((this[APPLICATION_KEY] as MoviesNightApp).recordsRepository) }
        }
    }
}
