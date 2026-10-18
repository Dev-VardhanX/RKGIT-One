package com.rkgit.one.ui.screens.materials

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rkgit.one.ui.components.ScreenScaffold

private data class YearEntry(val label: String, val year: Int)

private val years = listOf(
    YearEntry("First Year", 1),
    YearEntry("Second Year", 2),
    YearEntry("Third Year", 3),
    YearEntry("Fourth Year", 4)
)

@Composable
fun NotesScreen(
    onSemesterClick: (year: Int, semester: Int) -> Unit,
    onDownloadsClick: () -> Unit
) {
    ScreenScaffold(
        title = "Notes",
        actions = {
            IconButton(onClick = onDownloadsClick) {
                Icon(Icons.Outlined.Download, contentDescription = "Downloads")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(years) { entry ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(entry.label, style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            val sem1 = entry.year * 2 - 1
                            val sem2 = entry.year * 2
                            FilledTonalButton(
                                onClick = { onSemesterClick(entry.year, sem1) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Semester $sem1")
                            }
                            FilledTonalButton(
                                onClick = { onSemesterClick(entry.year, sem2) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Semester $sem2")
                            }
                        }
                    }
                }
            }
        }
    }
}
