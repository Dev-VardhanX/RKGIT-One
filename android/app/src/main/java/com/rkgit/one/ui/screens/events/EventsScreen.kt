package com.rkgit.one.ui.screens.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.rkgit.one.domain.model.Event
import com.rkgit.one.ui.components.EmptyView
import com.rkgit.one.ui.components.LoadableContent
import com.rkgit.one.ui.components.ScreenScaffold
import com.rkgit.one.ui.util.toDisplay

@Composable
fun EventsScreen(
    onEventClick: (String) -> Unit,
    viewModel: EventsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()

    ScreenScaffold("Events") { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            LoadableContent(
                state = state,
                refreshing = refreshing,
                onRefresh = viewModel::refresh,
                onRetry = viewModel::load,
                refreshErrors = viewModel.refreshErrors
            ) { data ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (data.upcoming.isEmpty() && data.recent.isEmpty()) {
                        item {
                            Box(Modifier.fillParentMaxSize()) { EmptyView("No events yet. Check back soon.") }
                        }
                    } else {
                        if (data.upcoming.isNotEmpty()) {
                            item { SectionHeader("Upcoming") }
                            items(data.upcoming, key = { it.id }) { EventCard(it, onEventClick) }
                        } else {
                            item {
                                Text(
                                    "No upcoming events right now.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (data.recent.isNotEmpty()) {
                            item { SectionHeader("Recent") }
                            items(data.recent, key = { it.id }) { EventCard(it, onEventClick) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EventCard(event: Event, onClick: (String) -> Unit) {
    Card(onClick = { onClick(event.id) }, modifier = Modifier.fillMaxWidth()) {
        Column {
            event.posterUrl?.let {
                AsyncImage(
                    model = it,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                )
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    event.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    event.eventDate.toDisplay(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                event.venue?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
