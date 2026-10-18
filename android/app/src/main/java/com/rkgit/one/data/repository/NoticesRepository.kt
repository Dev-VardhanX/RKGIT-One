package com.rkgit.one.data.repository

import com.rkgit.one.data.remote.dto.NoticeDto
import com.rkgit.one.data.remote.dto.toDomain
import com.rkgit.one.domain.model.Notice
import com.rkgit.one.domain.model.activeAt
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoticesRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    suspend fun active(now: Instant = Instant.now()): List<Notice> =
        supabase.from("notices").select {
            order("pinned", Order.DESCENDING)
            order("published_at", Order.DESCENDING)
        }.decodeList<NoticeDto>().map { it.toDomain() }.activeAt(now)

    suspend fun byId(id: String): Notice =
        supabase.from("notices").select {
            filter { eq("id", id) }
        }.decodeSingle<NoticeDto>().toDomain()
}
