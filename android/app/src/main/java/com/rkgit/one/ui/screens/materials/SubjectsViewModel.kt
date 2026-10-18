package com.rkgit.one.ui.screens.materials

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.rkgit.one.data.repository.MaterialsRepository
import com.rkgit.one.domain.model.Subject
import com.rkgit.one.ui.LoadableViewModel
import com.rkgit.one.ui.navigation.SubjectsRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SubjectsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MaterialsRepository
) : LoadableViewModel<List<Subject>>() {

    private val route = savedStateHandle.toRoute<SubjectsRoute>()
    val semester: Int = route.semester

    override suspend fun fetch(): List<Subject> =
        repository.subjects(route.year, route.semester)

    init { load() }
}
