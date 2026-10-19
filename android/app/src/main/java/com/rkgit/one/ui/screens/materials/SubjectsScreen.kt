package com.rkgit.one.ui.screens.materials

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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rkgit.one.domain.model.Subject
import com.rkgit.one.ui.components.EmptyView
import com.rkgit.one.ui.components.LoadableContent
import com.rkgit.one.ui.components.ScreenScaffold

@Composable
fun SubjectsScreen(
    onBack: () -> Unit,
    onSubjectClick: (Subject) -> Unit,
    viewModel: SubjectsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()

    ScreenScaffold("Semester ${viewModel.semester}", onBack = onBack) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            LoadableContent(
                state = state,
                refreshing = refreshing,
                onRefresh = viewModel::refresh,
                onRetry = viewModel::load,
                refreshErrors = viewModel.refreshErrors
            ) { subjects ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (subjects.isEmpty()) {
                        item {
                            Box(Modifier.fillParentMaxSize()) {
                                EmptyView("No study materials for this semester yet.")
                            }
                        }
                    } else {
                        items(subjects, key = { it.id }) { subject ->
                            SubjectCard(subject, onClick = { onSubjectClick(subject) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectCard(subject: Subject, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(subject.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (subject.materialCount == 1) "1 file" else "${subject.materialCount} files",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
