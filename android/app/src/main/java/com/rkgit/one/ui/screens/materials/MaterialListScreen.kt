package com.rkgit.one.ui.screens.materials

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DownloadDone
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rkgit.one.data.downloads.downloadKey
import com.rkgit.one.domain.model.StudyMaterial
import com.rkgit.one.ui.components.EmptyView
import com.rkgit.one.ui.components.LoadableContent
import com.rkgit.one.ui.components.ScreenScaffold
import com.rkgit.one.ui.navigation.ViewerRoute
import com.rkgit.one.ui.navigation.toViewerRoute
import com.rkgit.one.ui.util.toDateDisplay
import com.rkgit.one.ui.util.toFileSize

@Composable
fun MaterialListScreen(
    onBack: () -> Unit,
    onMaterialClick: (ViewerRoute) -> Unit,
    viewModel: MaterialListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val downloadedKeys by viewModel.downloadedKeys.collectAsStateWithLifecycle()

    ScreenScaffold(viewModel.subjectName, onBack = onBack) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            LoadableContent(
                state = state,
                refreshing = refreshing,
                onRefresh = viewModel::refresh,
                onRetry = viewModel::load,
                refreshErrors = viewModel.refreshErrors
            ) { materials ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (materials.isEmpty()) {
                        item {
                            Box(Modifier.fillParentMaxSize()) {
                                EmptyView("No files for this subject yet.")
                            }
                        }
                    } else {
                        items(materials, key = { it.id }) { material ->
                            val isDownloaded = downloadedKeys.contains(downloadKey("material", material.id))
                            MaterialCard(
                                material = material,
                                isDownloaded = isDownloaded,
                                onClick = {
                                    onMaterialClick(
                                        material.toViewerRoute(
                                            viewModel.subjectName,
                                            viewModel.year,
                                            viewModel.semester
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MaterialCard(
    material: StudyMaterial,
    isDownloaded: Boolean,
    onClick: () -> Unit
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                material.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            material.description?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${material.typeLabel} · ${material.fileSize.toFileSize()} · ${material.createdAt.toDateDisplay()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isDownloaded) {
                    Icon(
                        Icons.Outlined.DownloadDone,
                        contentDescription = "Downloaded",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
