package com.rkgit.one.data.repository

import com.rkgit.one.data.profile.DepartmentsCacheStore
import com.rkgit.one.data.profile.activeSorted
import com.rkgit.one.data.remote.dto.DepartmentDto
import com.rkgit.one.data.remote.dto.toDomain
import com.rkgit.one.domain.model.Department
import com.rkgit.one.ui.isOfflineError
import com.rkgit.one.ui.toUserMessage
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

sealed interface DepartmentsResult {
    data class Fresh(val list: List<Department>) : DepartmentsResult
    data class Cached(val list: List<Department>, val offline: Boolean) : DepartmentsResult
    data class Failed(val offline: Boolean, val message: String) : DepartmentsResult
}

@Singleton
class DepartmentsRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val cache: DepartmentsCacheStore
) {
    suspend fun load(): DepartmentsResult {
        return try {
            val list = supabase.from("departments").select {
                order("sort_order", Order.ASCENDING)
            }.decodeList<DepartmentDto>().map { it.toDomain() }.activeSorted()
            cache.write(list)
            DepartmentsResult.Fresh(list)
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            val cached = cache.read()
            if (cached.isNotEmpty()) {
                DepartmentsResult.Cached(cached, offline = e.isOfflineError())
            } else {
                DepartmentsResult.Failed(offline = e.isOfflineError(), message = e.toUserMessage())
            }
        }
    }
}
