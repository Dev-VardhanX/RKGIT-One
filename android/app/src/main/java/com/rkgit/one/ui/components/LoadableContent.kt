package com.rkgit.one.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.rkgit.one.ui.UiState
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> LoadableContent(
    state: UiState<T>,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    refreshErrors: Flow<String>? = null,
    content: @Composable (T) -> Unit
) {
    if (refreshErrors != null) {
        val context = LocalContext.current
        LaunchedEffect(refreshErrors) {
            refreshErrors.collect { message ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    when (state) {
        UiState.Loading -> LoadingView()
        is UiState.Error -> ErrorView(state.message, onRetry = onRetry, offline = state.offline)
        is UiState.Success -> PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            content(state.data)
        }
    }
}

