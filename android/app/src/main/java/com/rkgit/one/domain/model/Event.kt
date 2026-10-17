package com.rkgit.one.domain.model

import java.time.Instant

data class Event(
    val id: String,
    val title: String,
    val description: String?,
    val eventDate: Instant,
    val venue: String?,
    val posterUrl: String?,
    val registrationUrl: String?
)
