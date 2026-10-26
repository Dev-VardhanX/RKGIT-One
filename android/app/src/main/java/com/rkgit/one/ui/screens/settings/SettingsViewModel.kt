package com.rkgit.one.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rkgit.one.data.profile.ProfileRepository
import com.rkgit.one.data.profile.StudentProfile
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
class SettingsViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val departmentsRepository: DepartmentsRepository
) : ViewModel() {

    val profile: StateFlow<StudentProfile> = profileRepository.profile

    private val _departments = MutableStateFlow<DepartmentsUiState>(DepartmentsUiState.Loading)
    val departments: StateFlow<DepartmentsUiState> = _departments.asStateFlow()

    init {
        loadDepartments()
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

    fun setYear(year: Int?) {
        profileRepository.setYear(year)
    }

    fun setDepartment(code: String?) {
        profileRepository.setDepartment(code)
    }
}
