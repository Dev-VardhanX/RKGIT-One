package com.rkgit.one.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rkgit.one.ui.UiState
import com.rkgit.one.ui.components.ErrorView
import com.rkgit.one.ui.components.LoadingView

@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    when (val s = state) {
        UiState.Loading -> LoadingView()
        is UiState.Error -> ErrorView(s.message, onRetry = viewModel::load)
        is UiState.Success -> Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Connected to Supabase", style = MaterialTheme.typography.titleLarge)
            Text("${s.data.notes} notes, ${s.data.events} events, ${s.data.notices} notices")
        }
    }
}
