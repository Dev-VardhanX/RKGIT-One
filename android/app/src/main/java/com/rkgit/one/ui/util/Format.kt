package com.rkgit.one.ui.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy · h:mm a", Locale.getDefault())

fun Instant.toDisplay(): String = atZone(ZoneId.systemDefault()).format(dateTimeFormatter)
