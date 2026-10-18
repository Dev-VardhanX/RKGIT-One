package com.rkgit.one.data.files

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ViewerPrefs @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("viewer_prefs", Context.MODE_PRIVATE)

    fun getPage(key: String): Int = prefs.getInt(key, 0)

    fun setPage(key: String, page: Int) {
        prefs.edit().putInt(key, page).apply()
    }
}
