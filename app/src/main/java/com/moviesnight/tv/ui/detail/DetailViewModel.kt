package com.moviesnight.tv.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import com.moviesnight.tv.MoviesNightApp
import com.moviesnight.tv.data.RecordsRepository
import com.moviesnight.tv.data.model.RecordDetail
import com.moviesnight.tv.ui.DetailRoute
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data object Error : DetailUiState
    data class Loaded(val record: RecordDetail) : DetailUiState
}

class DetailViewModel(
    private val id: Long,
    private val repository: RecordsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = DetailUiState.Loading
        viewModelScope.launch {
            _state.value = try {
                DetailUiState.Loaded(repository.detail(id))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                DetailUiState.Error
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                DetailViewModel(
                    id = createSavedStateHandle().toRoute<DetailRoute>().id,
                    repository = (this[APPLICATION_KEY] as MoviesNightApp).recordsRepository,
                )
            }
        }
    }
}
