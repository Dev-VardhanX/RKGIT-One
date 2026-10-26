package com.rkgit.one.ui.screens.setup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rkgit.one.data.profile.ProfileRepository
import com.rkgit.one.data.repository.DepartmentsRepository
import com.rkgit.one.data.repository.DepartmentsResult
import com.rkgit.one.ui.screens.profile.DepartmentsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val profileRepository: ProfileRepository,
    private val departmentsRepository: DepartmentsRepository
) : ViewModel() {

    val selectedYear: StateFlow<Int?> = savedStateHandle.getStateFlow("selected_year", null)
    val selectedDepartmentCode: StateFlow<String?> = savedStateHandle.getStateFlow("selected_department_code", null)

    private val _departments = MutableStateFlow<DepartmentsUiState>(DepartmentsUiState.Loading)
    val departments: StateFlow<DepartmentsUiState> = _departments.asStateFlow()

    init {
        loadDepartments()
    }

    fun selectYear(year: Int?) {
        savedStateHandle["selected_year"] = year
    }

    fun selectDepartment(code: String?) {
        savedStateHandle["selected_department_code"] = code
    }

    fun finish() {
        profileRepository.save(selectedYear.value, selectedDepartmentCode.value)
    }

    fun skip() {
        profileRepository.skip()
    }

    fun retry() {
        loadDepartments()
    }

    private fun loadDepartments() {
        viewModelScope.launch {
            _departments.value = DepartmentsUiState.Loading
            val result = departmentsRepository.load()
            _departments.value = when (result) {
                is DepartmentsResult.Fresh -> DepartmentsUiState.Loaded(result.list, fromCache = false)
                is DepartmentsResult.Cached -> DepartmentsUiState.Loaded(result.list, fromCache = true)
                is DepartmentsResult.Failed -> DepartmentsUiState.Failed(result.message, result.offline)
            }
        }
    }
}
