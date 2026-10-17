package com.rkgit.one.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rkgit.one.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
private data class IdRow(val id: String)

data class Counts(val notes: Int, val events: Int, val notices: Int)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val supabase: SupabaseClient
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<Counts>>(UiState.Loading)
    val state: StateFlow<UiState<Counts>> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = try {
                suspend fun count(table: String) =
                    supabase.from(table).select(Columns.list("id")).decodeList<IdRow>().size
                UiState.Success(Counts(count("study_materials"), count("events"), count("notices")))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Could not reach the server.")
            }
        }
    }
}
