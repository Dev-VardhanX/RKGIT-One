package com.rkgit.one.ui.screens.materials

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.rkgit.one.data.downloads.DownloadRepository
import com.rkgit.one.data.repository.MaterialsRepository
import com.rkgit.one.domain.model.StudyMaterial
import com.rkgit.one.ui.LoadableViewModel
import com.rkgit.one.ui.navigation.MaterialListRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MaterialListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MaterialsRepository,
    downloadRepository: DownloadRepository
) : LoadableViewModel<List<StudyMaterial>>() {

    private val route = savedStateHandle.toRoute<MaterialListRoute>()
    val subjectName: String = route.subjectName
    val year: Int = route.year
    val semester: Int = route.semester

    val downloadedKeys: StateFlow<Set<String>> = downloadRepository
        .observeAllKeys()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    override suspend fun fetch(): List<StudyMaterial> =
        repository.materials(route.subjectId)

    init { load() }
}
