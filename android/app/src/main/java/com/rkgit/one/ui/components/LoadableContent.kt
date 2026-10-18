package com.rkgit.one.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rkgit.one.ui.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> LoadableContent(
    state: UiState<T>,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    content: @Composable (T) -> Unit
) {
    when (state) {
        UiState.Loading -> LoadingView()
        is UiState.Error -> ErrorView(state.message, onRetry = onRetry)
        is UiState.Success -> PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            content(state.data)
        }
    }
}
