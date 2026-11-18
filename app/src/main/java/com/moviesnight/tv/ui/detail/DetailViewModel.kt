package com.moviesnight.tv.ui.detail

import android.util.Log
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
import com.moviesnight.tv.data.remote.TmdbApi
import com.moviesnight.tv.ui.DetailRoute
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data object Error : DetailUiState
    /** [description] is TMDB's overview; null when there's none or TMDB couldn't be reached. */
    data class Loaded(val record: RecordDetail, val description: String? = null) : DetailUiState
}

class DetailViewModel(
    private val id: Long,
    private val repository: RecordsRepository,
    private val tmdb: TmdbApi,
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
                val record = repository.detail(id)
                DetailUiState.Loaded(record, description(record))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                DetailUiState.Error
            }
        }
    }

    /**
     * Fetched before showing the screen so the text below doesn't jump when it arrives.
     * It's only extra, so a slow or failing TMDB is skipped instead of blocking the record.
     */
    private suspend fun description(record: RecordDetail): String? {
        val tmdbId = record.tmdbId ?: return null
        return try {
            withTimeoutOrNull(TMDB_TIMEOUT_MS) { tmdb.overview(tmdbId, record.mediaType) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "TMDB description for ${record.tmdbId} failed", e)
            null
        }
    }

    companion object {
        private const val TAG = "DetailViewModel"
        private const val TMDB_TIMEOUT_MS = 3_000L

        val Factory = viewModelFactory {
            initializer {
                DetailViewModel(
                    id = createSavedStateHandle().toRoute<DetailRoute>().id,
                    repository = (this[APPLICATION_KEY] as MoviesNightApp).recordsRepository,
                    tmdb = (this[APPLICATION_KEY] as MoviesNightApp).tmdbApi,
                )
            }
        }
    }
}
