package com.rkgit.one.data.profile

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val store: ProfileStore
) {
    private val _profile = MutableStateFlow(store.read())
    val profile: StateFlow<StudentProfile> = _profile.asStateFlow()

    fun isSetupDone(): Boolean = store.read().setupDone

    fun save(year: Int?, departmentCode: String?) {
        val newProfile = StudentProfile(
            setupDone = true,
            year = if (isValidYear(year)) year else null,
            departmentCode = if (!departmentCode.isNullOrBlank()) departmentCode else null
        )
        store.write(newProfile)
        _profile.value = newProfile
    }

    fun skip() = save(null, null)

    fun setYear(year: Int?) {
        val currentDept = _profile.value.departmentCode
        save(year, currentDept)
    }

    fun setDepartment(code: String?) {
        val currentYear = _profile.value.year
        save(currentYear, code)
    }
}
