package com.rkgit.one.ui.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy · h:mm a", Locale.getDefault())

fun Instant.toDisplay(): String = atZone(ZoneId.systemDefault()).format(dateTimeFormatter)

private val dateOnlyFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())

fun Instant.toDateDisplay(): String = atZone(ZoneId.systemDefault()).format(dateOnlyFormatter)

fun Long.toFileSize(): String =
    if (this < 1024L * 1024L) "${maxOf(1L, (this + 512L) / 1024L)} KB"
    else String.format(Locale.getDefault(), "%.1f MB", this / 1024.0 / 1024.0)
