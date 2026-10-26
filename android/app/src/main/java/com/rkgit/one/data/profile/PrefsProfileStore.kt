package com.rkgit.one.data.profile

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrefsProfileStore @Inject constructor(
    @ApplicationContext context: Context
) : ProfileStore {
    private val prefs = context.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE)

    override fun read(): StudentProfile {
        val setupDone = prefs.getBoolean("setup_done", false)
        val rawYear = prefs.getInt("year", 0)
        val rawDept = prefs.getString("department", null)
        return StudentProfile(
            setupDone = setupDone,
            year = sanitizeStoredYear(rawYear),
            departmentCode = sanitizeStoredDepartment(rawDept)
        )
    }

    override fun write(profile: StudentProfile) {
        val y = if (isValidYear(profile.year)) profile.year!! else 0
        val d = if (profile.departmentCode.isNullOrBlank()) "" else profile.departmentCode
        prefs.edit()
            .putBoolean("setup_done", profile.setupDone)
            .putInt("year", y)
            .putString("department", d)
            .apply()
    }
}
