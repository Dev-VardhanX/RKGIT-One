package com.rkgit.one.ui

import com.rkgit.one.data.files.DownloadException

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String, val offline: Boolean = false) : UiState<Nothing>
}

fun Throwable.isOfflineError(): Boolean =
    generateSequence(this) { it.cause }.any { it is java.io.IOException && it !is DownloadException }

fun Throwable.toUserMessage(): String =
    if (isOfflineError()) "Can't reach the server. Check your internet connection and try again."
    else "Something went wrong. Please try again."

fun Throwable.toRefreshMessage(): String =
    if (isOfflineError()) "Can't refresh. You're offline."
    else "Couldn't refresh. Please try again."

