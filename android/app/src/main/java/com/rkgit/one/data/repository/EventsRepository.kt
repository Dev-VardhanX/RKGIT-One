package com.rkgit.one.data.repository

import com.rkgit.one.data.remote.dto.EventDto
import com.rkgit.one.data.remote.dto.toDomain
import com.rkgit.one.domain.model.Event
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventsRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    /** Events that have not started yet, soonest first. */
    suspend fun upcoming(now: Instant = Instant.now()): List<Event> =
        supabase.from("events").select {
            filter { gte("event_date", now.toString()) }
            order("event_date", Order.ASCENDING)
        }.decodeList<EventDto>().map { it.toDomain() }

    /** The most recent past events, newest first. */
    suspend fun recent(now: Instant = Instant.now(), limit: Long = 10): List<Event> =
        supabase.from("events").select {
            filter { lt("event_date", now.toString()) }
            order("event_date", Order.DESCENDING)
            limit(limit)
        }.decodeList<EventDto>().map { it.toDomain() }

    suspend fun byId(id: String): Event =
        supabase.from("events").select {
            filter { eq("id", id) }
        }.decodeSingle<EventDto>().toDomain()
}
