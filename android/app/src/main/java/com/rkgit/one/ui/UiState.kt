package com.rkgit.one.ui

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

fun Throwable.toUserMessage(): String = when (this) {
    is java.io.IOException -> "Can't reach the server. Check your internet connection and try again."
    else -> "Something went wrong. Please try again."
}
