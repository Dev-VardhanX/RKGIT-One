package com.rkgit.one.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rkgit.one.domain.model.RecentMaterial
import com.rkgit.one.ui.components.EmptyView
import com.rkgit.one.ui.components.LoadableContent
import com.rkgit.one.ui.components.ScreenScaffold
import com.rkgit.one.ui.screens.events.EventCard
import com.rkgit.one.ui.screens.notices.NoticeCard
import com.rkgit.one.ui.util.toDateDisplay
import com.rkgit.one.ui.util.toFileSize

@Composable
fun HomeScreen(
    onNoticeClick: (String) -> Unit,
    onEventClick: (String) -> Unit,
    onMaterialClick: (subjectId: String, subjectName: String, year: Int, semester: Int) -> Unit,
    onSeeAllNotices: () -> Unit,
    onSeeAllNotes: () -> Unit,
    onSeeAllEvents: () -> Unit,
    onDownloadsClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()

    ScreenScaffold(
        title = "RKGIT ONE",
        actions = {
            IconButton(onClick = onDownloadsClick) {
                Icon(Icons.Outlined.Download, contentDescription = "Downloads")
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            LoadableContent(
                state = state,
                refreshing = refreshing,
                onRefresh = viewModel::refresh,
                onRetry = viewModel::load
            ) { data ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (data.isEmpty) {
                        item(key = "empty_state") {
                            Box(Modifier.fillParentMaxSize()) {
                                EmptyView("Nothing here yet. Check back soon.")
                            }
                        }
                    } else {
                        if (data.notices.isNotEmpty()) {
                            item(key = "header_notices") {
                                SectionHeader(title = "Latest notices", onSeeAll = onSeeAllNotices)
                            }
                            items(data.notices, key = { "notice_${it.id}" }) { notice ->
                                NoticeCard(notice = notice, onClick = { onNoticeClick(notice.id) })
                            }
                        }

                        if (data.materials.isNotEmpty()) {
                            item(key = "header_notes") {
                                SectionHeader(title = "Recent notes", onSeeAll = onSeeAllNotes)
                            }
                            items(data.materials, key = { "material_${it.material.id}" }) { material ->
                                RecentMaterialCard(
                                    recent = material,
                                    onClick = {
                                        onMaterialClick(
                                            material.material.subjectId,
                                            material.subjectName,
                                            material.year,
                                            material.semester
                                        )
                                    }
                                )
                            }
                        }

                        if (data.events.isNotEmpty()) {
                            item(key = "header_events") {
                                SectionHeader(title = "Upcoming events", onSeeAll = onSeeAllEvents)
                            }
                            items(data.events, key = { "event_${it.id}" }) { event ->
                                EventCard(event = event, onClick = { onEventClick(event.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    onSeeAll: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = onSeeAll) {
            Text("See all")
        }
    }
}

@Composable
private fun RecentMaterialCard(
    recent: RecentMaterial,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = recent.material.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${recent.subjectName} · Semester ${recent.semester}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "${recent.material.typeLabel} · ${recent.material.fileSize.toFileSize()} · ${recent.material.createdAt.toDateDisplay()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
