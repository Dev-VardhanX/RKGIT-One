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
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rkgit.one.domain.model.RecentMaterial
import com.rkgit.one.push.isAllowed
import com.rkgit.one.push.rememberNotificationPermission
import com.rkgit.one.push.settingsAfterPermissionResult
import com.rkgit.one.push.shouldShowFirstPrompt
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
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val promptShown by viewModel.promptShown.collectAsStateWithLifecycle()

    val controller = rememberNotificationPermission { granted ->
        val current = viewModel.notificationSettings.value
        val next = settingsAfterPermissionResult(
            granted = granted,
            current = current,
            requestedNotices = true,
            requestedEvents = true
        )
        viewModel.setBothNotifications(next.noticesEnabled, next.eventsEnabled)
    }

    val permState = controller.state
    val setupDone = profile.setupDone

    LaunchedEffect(setupDone, promptShown, permState) {
        if (setupDone && !promptShown && isAllowed(permState)) {
            viewModel.markPromptShown()
        }
    }

    if (shouldShowFirstPrompt(setupDone = setupDone, promptShown = promptShown, state = permState)) {
        AlertDialog(
            onDismissRequest = {
                viewModel.markPromptShown()
                viewModel.setBothNotifications(notices = false, events = false)
            },
            title = { Text("Get notified?") },
            text = {
                Text("Turn on notifications to hear about new notices and events for your year and branch.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.markPromptShown()
                        controller.request()
                    }
                ) {
                    Text("Turn on")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.markPromptShown()
                        viewModel.setBothNotifications(notices = false, events = false)
                    }
                ) {
                    Text("Not now")
                }
            }
        )
    }

    ScreenScaffold(
        title = "RKGIT ONE",
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Outlined.Search, contentDescription = "Search")
            }
            IconButton(onClick = onDownloadsClick) {
                Icon(Icons.Outlined.Download, contentDescription = "Downloads")
            }
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Outlined.Settings, contentDescription = "Settings")
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            LoadableContent(
                state = state,
                refreshing = refreshing,
                onRefresh = viewModel::refresh,
                onRetry = viewModel::load,
                refreshErrors = viewModel.refreshErrors
            ) { data ->
                val visibleNotices = data.visibleNotices(profile.year, profile.departmentCode)
                val empty = data.isEmptyFor(profile.year, profile.departmentCode)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (empty) {
                        item(key = "empty_state") {
                            Box(Modifier.fillParentMaxSize()) {
                                EmptyView("Nothing here yet. Check back soon.")
                            }
                        }
                    } else {
                        if (visibleNotices.isNotEmpty()) {
                            item(key = "header_notices") {
                                SectionHeader(title = "Latest notices", onSeeAll = onSeeAllNotices)
                            }
                            items(visibleNotices, key = { "notice_${it.id}" }) { notice ->
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
                text = recent.material.type.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
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
