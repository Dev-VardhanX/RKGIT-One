package com.rkgit.one.data.profile

import android.content.Context
import com.rkgit.one.domain.model.Department
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrefsDepartmentsCacheStore @Inject constructor(
    @ApplicationContext context: Context
) : DepartmentsCacheStore {
    private val prefs = context.getSharedPreferences("departments_cache", Context.MODE_PRIVATE)

    override fun read(): List<Department> {
        val json = prefs.getString("json", null)
        return decodeDepartments(json)
    }

    override fun write(list: List<Department>) {
        prefs.edit()
            .putString("json", encodeDepartments(list))
            .apply()
    }
}
