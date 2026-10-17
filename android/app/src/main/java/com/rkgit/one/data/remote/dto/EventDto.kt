package com.rkgit.one.data.remote.dto

import com.rkgit.one.data.remote.PublicUrls
import com.rkgit.one.domain.model.Event
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime

@Serializable
data class EventDto(
    val id: String,
    val title: String,
    val description: String? = null,
    @SerialName("event_date") val eventDate: String,
    val venue: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("registration_url") val registrationUrl: String? = null
)

fun EventDto.toDomain() = Event(
    id = id,
    title = title,
    description = description?.takeIf { it.isNotBlank() },
    eventDate = OffsetDateTime.parse(eventDate).toInstant(),
    venue = venue?.takeIf { it.isNotBlank() },
    posterUrl = posterPath?.takeIf { it.isNotBlank() }?.let { PublicUrls.file("event-posters", it) },
    registrationUrl = registrationUrl?.takeIf { it.isNotBlank() }
)
