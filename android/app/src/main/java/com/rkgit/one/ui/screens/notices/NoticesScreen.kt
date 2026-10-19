package com.rkgit.one.ui.screens.notices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rkgit.one.domain.model.Notice
import com.rkgit.one.domain.model.NoticeCategory
import com.rkgit.one.ui.components.EmptyView
import com.rkgit.one.ui.components.LoadableContent
import com.rkgit.one.ui.components.ScreenScaffold
import com.rkgit.one.ui.util.toDateDisplay

@Composable
fun NoticesScreen(
    onNoticeClick: (String) -> Unit,
    viewModel: NoticesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()

    var selectedValue by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedCategory = selectedValue?.let { NoticeCategory.fromValue(it) }

    ScreenScaffold("Notices") { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            LoadableContent(
                state = state,
                refreshing = refreshing,
                onRefresh = viewModel::refresh,
                onRetry = viewModel::load,
                refreshErrors = viewModel.refreshErrors
            ) { notices ->
                Column(Modifier.fillMaxSize()) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedValue == null,
                                onClick = { selectedValue = null },
                                label = { Text("All") }
                            )
                        }
                        items(NoticeCategory.entries, key = { it.value }) { category ->
                            FilterChip(
                                selected = selectedValue == category.value,
                                onClick = { selectedValue = category.value },
                                label = { Text(category.label) }
                            )
                        }
                    }

                    val filteredNotices = if (selectedCategory == null) {
                        notices
                    } else {
                        notices.filter { it.category == selectedCategory }
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (notices.isEmpty()) {
                            item {
                                Box(Modifier.fillParentMaxSize()) {
                                    EmptyView("No notices right now.")
                                }
                            }
                        } else if (filteredNotices.isEmpty()) {
                            item {
                                Box(Modifier.fillParentMaxSize()) {
                                    EmptyView("No notices in this category.")
                                }
                            }
                        } else {
                            items(filteredNotices, key = { it.id }) { notice ->
                                NoticeCard(
                                    notice = notice,
                                    onClick = { onNoticeClick(notice.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun NoticeCard(
    notice: Notice,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (notice.pinned) {
                    Icon(
                        Icons.Filled.PushPin,
                        contentDescription = "Pinned",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = notice.category.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = notice.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            notice.body?.let { body ->
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = notice.publishedAt.toDateDisplay(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                notice.attachment?.let { attachment ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Outlined.AttachFile,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = attachment.typeLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
