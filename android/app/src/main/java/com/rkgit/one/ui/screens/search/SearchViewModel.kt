package com.rkgit.one.ui.screens.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rkgit.one.data.downloads.DownloadRepository
import com.rkgit.one.data.local.DownloadedFile
import com.rkgit.one.data.repository.SearchRepository
import com.rkgit.one.data.search.MAX_QUERY_LENGTH
import com.rkgit.one.data.search.SEARCH_DEBOUNCE_MS
import com.rkgit.one.data.search.isSearchable
import com.rkgit.one.data.search.normalizeQuery
import com.rkgit.one.data.search.searchDownloads
import com.rkgit.one.domain.model.SearchResults
import com.rkgit.one.ui.isOfflineError
import com.rkgit.one.ui.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Results(val data: SearchResults, val query: String) : SearchUiState
    data class OfflineResults(val items: List<DownloadedFile>, val query: String) : SearchUiState
    data class Error(val message: String, val query: String) : SearchUiState
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val searchRepository: SearchRepository,
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    val initialQuery: String = savedStateHandle.get<String>("q") ?: ""

    private val queryFlow: StateFlow<String> = savedStateHandle.getStateFlow("q", initialQuery)

    private val retryTick = MutableStateFlow(0)

    fun onQueryChange(text: String) {
        savedStateHandle["q"] = text.take(MAX_QUERY_LENGTH)
    }

    fun retry() {
        retryTick.value++
    }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<SearchUiState> = combine(
        queryFlow.debounce(SEARCH_DEBOUNCE_MS),
        retryTick
    ) { rawQuery, tick ->
        normalizeQuery(rawQuery) to tick
    }.flatMapLatest { (q, _) ->
        flow {
            if (!isSearchable(q)) {
                emit(SearchUiState.Idle)
            } else {
                emit(SearchUiState.Loading)
                try {
                    val results = searchRepository.search(q)
                    emit(SearchUiState.Results(results, q))
                } catch (ce: CancellationException) {
                    throw ce
                } catch (e: Throwable) {
                    if (e.isOfflineError()) {
                        val downloaded = searchDownloads(downloadRepository.observeAll().first(), q)
                        emit(SearchUiState.OfflineResults(downloaded, q))
                    } else {
                        emit(SearchUiState.Error(e.toUserMessage(), q))
                    }
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, SearchUiState.Idle)
}
