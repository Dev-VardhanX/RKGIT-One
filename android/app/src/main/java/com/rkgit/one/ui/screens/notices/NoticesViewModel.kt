package com.rkgit.one.ui.screens.notices

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.rkgit.one.data.profile.ProfileRepository
import com.rkgit.one.data.repository.DepartmentsRepository
import com.rkgit.one.data.repository.DepartmentsResult
import com.rkgit.one.data.repository.NoticesRepository
import com.rkgit.one.domain.model.Department
import com.rkgit.one.domain.model.Notice
import com.rkgit.one.ui.LoadableViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AudienceFilter(
    val year: Int?,
    val departmentCode: String?
)

@HiltViewModel
class NoticesViewModel @Inject constructor(
    private val repository: NoticesRepository,
    private val profileRepository: ProfileRepository,
    private val departmentsRepository: DepartmentsRepository,
    private val savedStateHandle: SavedStateHandle
) : LoadableViewModel<List<Notice>>() {

    override suspend fun fetch(): List<Notice> = repository.active()

    private val _departments = MutableStateFlow<List<Department>>(emptyList())
    val departments: StateFlow<List<Department>> = _departments.asStateFlow()

    private val hasOverride = savedStateHandle.getStateFlow("has_override", false)
    private val overrideYear = savedStateHandle.getStateFlow<Int?>("override_year", null)
    private val overrideDept = savedStateHandle.getStateFlow<String?>("override_dept", null)

    val effectiveFilter: StateFlow<AudienceFilter> = combine(
        profileRepository.profile,
        hasOverride,
        overrideYear,
        overrideDept
    ) { profile, hasOver, overYear, overDept ->
        if (hasOver) {
            AudienceFilter(overYear, overDept)
        } else {
            AudienceFilter(profile.year, profile.departmentCode)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AudienceFilter(
            profileRepository.profile.value.year,
            profileRepository.profile.value.departmentCode
        )
    )

    init {
        load()

        viewModelScope.launch {
            when (val result = departmentsRepository.load()) {
                is DepartmentsResult.Fresh -> _departments.value = result.list
                is DepartmentsResult.Cached -> _departments.value = result.list
                is DepartmentsResult.Failed -> _departments.value = emptyList()
            }
        }

        viewModelScope.launch {
            var isInitial = true
            profileRepository.profile.collect { p ->
                if (isInitial) {
                    isInitial = false
                    val lastYear = savedStateHandle.get<Int>("last_profile_year")
                    val lastDept = savedStateHandle.get<String>("last_profile_dept")
                    val hadSaved = savedStateHandle.contains("last_profile_year") || savedStateHandle.contains("last_profile_dept")
                    if (hadSaved && (lastYear != p.year || lastDept != p.departmentCode)) {
                        clearOverride()
                    }
                    savedStateHandle["last_profile_year"] = p.year
                    savedStateHandle["last_profile_dept"] = p.departmentCode
                } else {
                    savedStateHandle["last_profile_year"] = p.year
                    savedStateHandle["last_profile_dept"] = p.departmentCode
                    clearOverride()
                }
            }
        }
    }

    private fun clearOverride() {
        savedStateHandle["has_override"] = false
        savedStateHandle["override_year"] = null
        savedStateHandle["override_dept"] = null
    }

    fun setYear(year: Int?) {
        val current = effectiveFilter.value
        savedStateHandle["has_override"] = true
        savedStateHandle["override_year"] = year
        savedStateHandle["override_dept"] = current.departmentCode
    }

    fun setDepartment(code: String?) {
        val current = effectiveFilter.value
        savedStateHandle["has_override"] = true
        savedStateHandle["override_year"] = current.year
        savedStateHandle["override_dept"] = code
    }

    fun showAll() {
        savedStateHandle["has_override"] = true
        savedStateHandle["override_year"] = null
        savedStateHandle["override_dept"] = null
    }
}
