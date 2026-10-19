package com.rkgit.one.ui.screens.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.rkgit.one.domain.model.Event
import com.rkgit.one.ui.UiState
import com.rkgit.one.ui.components.ErrorView
import com.rkgit.one.ui.components.LoadingView
import com.rkgit.one.ui.components.ScreenScaffold
import com.rkgit.one.ui.util.isSafeWebUrl
import com.rkgit.one.ui.util.openInCustomTab
import com.rkgit.one.ui.util.toDisplay

@Composable
fun EventDetailScreen(
    onBack: () -> Unit,
    viewModel: EventDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ScreenScaffold("Event", onBack = onBack) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            when (val s = state) {
                UiState.Loading -> LoadingView()
                is UiState.Error -> ErrorView(s.message, onRetry = viewModel::load, offline = s.offline)
                is UiState.Success -> EventContent(s.data)
            }
        }
    }
}

@Composable
private fun EventContent(event: Event) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        event.posterUrl?.let {
            AsyncImage(
                model = it,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
            )
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(event.title, style = MaterialTheme.typography.headlineSmall)
            InfoRow(Icons.Outlined.Event, event.eventDate.toDisplay())
            event.venue?.let { InfoRow(Icons.Outlined.Place, it) }
            event.description?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
            event.registrationUrl?.takeIf { it.isSafeWebUrl() }?.let { url ->
                Button(onClick = { context.openInCustomTab(url) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Register")
                }
            }
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}
