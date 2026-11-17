package com.moviesnight.tv.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.moviesnight.tv.MoviesNightApp
import com.moviesnight.tv.data.RecordsRepository
import com.moviesnight.tv.data.model.MediaFilter
import com.moviesnight.tv.data.model.RecordSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomeViewModel(repository: RecordsRepository) : ViewModel() {

    private val _selected = MutableStateFlow(MediaFilter.All)
    val selected: StateFlow<MediaFilter> = _selected.asStateFlow()

    /** One cached stream per tab, so switching tabs keeps what was already loaded. */
    val records: Map<MediaFilter, Flow<PagingData<RecordSummary>>> =
        MediaFilter.entries.associateWith { repository.pagedRecords(it).cachedIn(viewModelScope) }

    fun select(filter: MediaFilter) {
        _selected.value = filter
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { HomeViewModel((this[APPLICATION_KEY] as MoviesNightApp).recordsRepository) }
        }
    }
}
