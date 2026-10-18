package com.rkgit.one.ui.screens.downloads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rkgit.one.data.downloads.RemoteFileInfo
import com.rkgit.one.data.downloads.RemoteStatus
import com.rkgit.one.data.downloads.downloadKey
import com.rkgit.one.data.downloads.shouldConfirmMobileData
import com.rkgit.one.data.local.DownloadedFile
import com.rkgit.one.ui.components.EmptyView
import com.rkgit.one.ui.components.ScreenScaffold
import com.rkgit.one.ui.util.isActiveNetworkMetered
import com.rkgit.one.ui.util.toDateDisplay
import com.rkgit.one.ui.util.toFileSize
import java.time.Instant
import java.util.Locale

private fun yearName(year: Int?): String = when (year) {
    1 -> "First Year"
    2 -> "Second Year"
    3 -> "Third Year"
    4 -> "Fourth Year"
    else -> "Year $year"
}

private fun fileTypeLabel(fileType: String, fileName: String): String {
    if (fileType.startsWith("image/")) return "Image"
    val ext = fileName.substringAfterLast('.', "")
    return if (ext.isNotBlank()) ext.uppercase(Locale.getDefault()) else "File"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    onBack: () -> Unit,
    onOpen: (DownloadedFile) -> Unit,
    viewModel: DownloadsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var itemToRemove by remember { mutableStateOf<DownloadedFile?>(null) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    var pendingMobileUpdate by remember { mutableStateOf<Pair<DownloadedFile, RemoteFileInfo>?>(null) }

    ScreenScaffold(
        title = "Downloads",
        onBack = onBack,
        actions = {
            if (uiState.count > 0) {
                IconButton(onClick = { showClearAllDialog = true }) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteSweep,
                        contentDescription = "Clear all downloads"
                    )
                }
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.refreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (uiState.count == 0) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        Box(Modifier.fillParentMaxSize()) {
                            EmptyView("No downloads yet. Open a file and tap Download to read it offline.")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(key = "summary_header") {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            val fileLabel = if (uiState.count == 1) "1 file" else "${uiState.count} files"
                            Text(
                                text = "$fileLabel · ${uiState.totalBytes.toFileSize()}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (uiState.checkFailed) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Couldn't check for updates. Pull down to retry.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    for (group in uiState.groups.notes) {
                        val groupHeader = if (group.year != null && group.semester != null) {
                            "${yearName(group.year)} · Semester ${group.semester}"
                        } else {
                            "Other notes"
                        }
                        item(key = "header_${group.year}_${group.semester}") {
                            Text(
                                text = groupHeader,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        items(
                            items = group.items,
                            key = { "${it.kind}:${it.itemId}" }
                        ) { row ->
                            val status = uiState.statuses[downloadKey(row.kind, row.itemId)] ?: RemoteStatus.UNKNOWN
                            val remoteInfo = uiState.remote[downloadKey(row.kind, row.itemId)]
                            DownloadItemCard(
                                row = row,
                                status = status,
                                remoteInfo = remoteInfo,
                                onOpen = { onOpen(row) },
                                onUpdateClick = { info ->
                                    if (shouldConfirmMobileData(info.fileSize, isActiveNetworkMetered(context))) {
                                        pendingMobileUpdate = Pair(row, info)
                                    } else {
                                        viewModel.update(row, info)
                                    }
                                },
                                onRemoveClick = {
                                    itemToRemove = row
                                }
                            )
                        }
                    }

                    if (uiState.groups.notices.isNotEmpty()) {
                        item(key = "header_notices") {
                            Text(
                                text = "Notice attachments",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        items(
                            items = uiState.groups.notices,
                            key = { "${it.kind}:${it.itemId}" }
                        ) { row ->
                            val status = uiState.statuses[downloadKey(row.kind, row.itemId)] ?: RemoteStatus.UNKNOWN
                            val remoteInfo = uiState.remote[downloadKey(row.kind, row.itemId)]
                            DownloadItemCard(
                                row = row,
                                status = status,
                                remoteInfo = remoteInfo,
                                onOpen = { onOpen(row) },
                                onUpdateClick = { info ->
                                    if (shouldConfirmMobileData(info.fileSize, isActiveNetworkMetered(context))) {
                                        pendingMobileUpdate = Pair(row, info)
                                    } else {
                                        viewModel.update(row, info)
                                    }
                                },
                                onRemoveClick = {
                                    itemToRemove = row
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (itemToRemove != null) {
        val item = itemToRemove!!
        AlertDialog(
            onDismissRequest = { itemToRemove = null },
            title = { Text("Remove this download?") },
            text = { Text(item.title) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = itemToRemove
                        itemToRemove = null
                        if (target != null) {
                            viewModel.remove(target)
                        }
                    }
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToRemove = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("Remove all downloads?") },
            text = { Text("${uiState.count} files will be deleted from this device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearAllDialog = false
                        viewModel.clearAll()
                    }
                ) {
                    Text("Remove all")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (pendingMobileUpdate != null) {
        val (_, info) = pendingMobileUpdate!!
        AlertDialog(
            onDismissRequest = { pendingMobileUpdate = null },
            title = { Text("Download on mobile data?") },
            text = { Text("This file is ${info.fileSize.toFileSize()} and you're not on Wi-Fi.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val update = pendingMobileUpdate
                        pendingMobileUpdate = null
                        if (update != null) {
                            viewModel.update(update.first, update.second)
                        }
                    }
                ) {
                    Text("Download")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingMobileUpdate = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DownloadItemCard(
    row: DownloadedFile,
    status: RemoteStatus,
    remoteInfo: RemoteFileInfo?,
    onOpen: () -> Unit,
    onUpdateClick: (RemoteFileInfo) -> Unit,
    onRemoveClick: () -> Unit
) {
    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = row.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (row.subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = row.subtitle,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                val typeLabel = fileTypeLabel(row.fileType, row.fileName)
                val dateStr = Instant.ofEpochMilli(row.downloadedAt).toDateDisplay()
                Text(
                    text = "$typeLabel · ${row.sizeBytes.toFileSize()} · Downloaded $dateStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                when (status) {
                    RemoteStatus.UPDATE_AVAILABLE -> {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Update available",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    RemoteStatus.UNAVAILABLE -> {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "No longer available online",
                            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> {}
                }
            }

            if (status == RemoteStatus.UPDATE_AVAILABLE && remoteInfo != null) {
                TextButton(
                    onClick = { onUpdateClick(remoteInfo) }
                ) {
                    Text("Update")
                }
            }

            IconButton(
                onClick = onRemoveClick
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Remove download"
                )
            }
        }
    }
}
