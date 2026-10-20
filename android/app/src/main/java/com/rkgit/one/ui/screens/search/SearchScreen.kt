package com.rkgit.one.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rkgit.one.data.local.DownloadedFile
import com.rkgit.one.data.search.SEARCH_PAGE_SIZE
import com.rkgit.one.domain.model.SearchEvent
import com.rkgit.one.domain.model.SearchMaterial
import com.rkgit.one.domain.model.SearchNotice
import com.rkgit.one.ui.components.EmptyView
import com.rkgit.one.ui.components.ErrorView
import com.rkgit.one.ui.components.LoadingView
import com.rkgit.one.ui.components.ScreenScaffold
import com.rkgit.one.ui.util.toDisplay
import com.rkgit.one.ui.util.toFileSize
import java.util.Locale

@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onMaterialClick: (SearchMaterial) -> Unit,
    onEventClick: (String) -> Unit,
    onNoticeClick: (String) -> Unit,
    onDownloadedClick: (DownloadedFile) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    var queryText by rememberSaveable { mutableStateOf(viewModel.initialQuery) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    ScreenScaffold(
        title = "Search",
        onBack = onBack,
        titleContent = {
            TextField(
                value = queryText,
                onValueChange = { newText ->
                    queryText = newText
                    viewModel.onQueryChange(newText)
                },
                placeholder = {
                    Text(
                        "Search notes, events, notices",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (queryText.isNotEmpty()) {
                        IconButton(onClick = {
                            queryText = ""
                            viewModel.onQueryChange("")
                        }) {
                            Icon(Icons.Outlined.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when (val state = uiState) {
                is SearchUiState.Idle -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Search notes, events, and notices. Type at least 2 characters.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(32.dp)
                        )
                    }
                }
                is SearchUiState.Loading -> {
                    LoadingView()
                }
                is SearchUiState.Results -> {
                    val data = state.data
                    if (data.total == 0) {
                        EmptyView("No results for “${state.query}”.")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (data.materials.isNotEmpty()) {
                                item(key = "notes_header") {
                                    SectionHeader("Notes (${data.materials.size})")
                                }
                                if (data.materials.size == SEARCH_PAGE_SIZE) {
                                    item(key = "notes_page_caption") {
                                        Text(
                                            text = "Showing the first 20. Try a more specific search.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                items(data.materials, key = { "material_${it.id}" }) { material ->
                                    MaterialCard(
                                        material = material,
                                        onClick = {
                                            keyboardController?.hide()
                                            onMaterialClick(material)
                                        }
                                    )
                                }
                            }

                            if (data.events.isNotEmpty()) {
                                item(key = "events_header") {
                                    SectionHeader("Events (${data.events.size})")
                                }
                                if (data.events.size == SEARCH_PAGE_SIZE) {
                                    item(key = "events_page_caption") {
                                        Text(
                                            text = "Showing the first 20. Try a more specific search.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                items(data.events, key = { "event_${it.id}" }) { event ->
                                    EventCard(
                                        event = event,
                                        onClick = {
                                            keyboardController?.hide()
                                            onEventClick(event.id)
                                        }
                                    )
                                }
                            }

                            if (data.notices.isNotEmpty()) {
                                item(key = "notices_header") {
                                    SectionHeader("Notices (${data.notices.size})")
                                }
                                if (data.notices.size == SEARCH_PAGE_SIZE) {
                                    item(key = "notices_page_caption") {
                                        Text(
                                            text = "Showing the first 20. Try a more specific search.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                items(data.notices, key = { "notice_${it.id}" }) { notice ->
                                    NoticeCard(
                                        notice = notice,
                                        onClick = {
                                            keyboardController?.hide()
                                            onNoticeClick(notice.id)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                is SearchUiState.OfflineResults -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item(key = "offline_banner") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "You're offline. Showing downloaded files only.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = viewModel::retry) {
                                    Text("Retry online search")
                                }
                            }
                        }
                        if (state.items.isEmpty()) {
                            item(key = "offline_empty") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 48.dp, horizontal = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No downloaded files match “${state.query}”.",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            item(key = "offline_header") {
                                SectionHeader("Downloaded files (${state.items.size})")
                            }
                            items(state.items, key = { "${it.kind}_${it.itemId}" }) { item ->
                                DownloadedCard(
                                    item = item,
                                    onClick = {
                                        keyboardController?.hide()
                                        onDownloadedClick(item)
                                    }
                                )
                            }
                        }
                    }
                }
                is SearchUiState.Error -> {
                    ErrorView(
                        message = state.message,
                        onRetry = viewModel::retry,
                        offline = false
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun MaterialCard(material: SearchMaterial, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = material.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${material.subjectName} · Year ${material.year} · Semester ${material.semester}",
                style = MaterialTheme.typography.bodyMedium
            )
            if (material.snippet != null) {
                Text(
                    text = material.snippet,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "${material.typeLabel} · ${material.fileSize.toFileSize()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EventCard(event: SearchEvent, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = event.eventDate.toDisplay(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            if (event.venue != null) {
                Text(
                    text = event.venue,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (event.snippet != null) {
                Text(
                    text = event.snippet,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun NoticeCard(notice: SearchNotice, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (notice.pinned) {
                    Icon(
                        Icons.Outlined.PushPin,
                        contentDescription = "Pinned",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                }
                Text(
                    text = notice.category.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (notice.hasAttachment) {
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        Icons.Outlined.AttachFile,
                        contentDescription = "Has attachment",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = notice.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (notice.snippet != null) {
                Text(
                    text = notice.snippet,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun downloadedTypeLabel(fileType: String, fileName: String): String {
    if (fileType.startsWith("image/")) return "Image"
    val ext = fileName.substringAfterLast('.', "")
    return if (ext.isNotBlank()) ext.uppercase(Locale.getDefault()) else "File"
}

@Composable
private fun DownloadedCard(item: DownloadedFile, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (item.subtitle.isNotBlank()) {
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            val typeLabel = downloadedTypeLabel(item.fileType, item.fileName)
            Text(
                text = "$typeLabel · ${item.sizeBytes.toFileSize()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
