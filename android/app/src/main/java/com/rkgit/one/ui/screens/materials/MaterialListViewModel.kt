package com.rkgit.one.ui.screens.materials

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.rkgit.one.data.repository.MaterialsRepository
import com.rkgit.one.domain.model.StudyMaterial
import com.rkgit.one.ui.LoadableViewModel
import com.rkgit.one.ui.navigation.MaterialListRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MaterialListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MaterialsRepository
) : LoadableViewModel<List<StudyMaterial>>() {

    private val route = savedStateHandle.toRoute<MaterialListRoute>()
    val subjectName: String = route.subjectName

    override suspend fun fetch(): List<StudyMaterial> =
        repository.materials(route.subjectId)

    init { load() }
}
