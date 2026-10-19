package com.rkgit.one.ui.screens.notices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.DownloadDone
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rkgit.one.domain.model.Notice
import com.rkgit.one.ui.UiState
import com.rkgit.one.ui.components.ErrorView
import com.rkgit.one.ui.components.LoadingView
import com.rkgit.one.ui.components.ScreenScaffold
import com.rkgit.one.ui.util.isSafeWebUrl
import com.rkgit.one.ui.util.openInCustomTab
import com.rkgit.one.ui.util.toDateDisplay
import com.rkgit.one.ui.util.toFileSize

@Composable
fun NoticeDetailScreen(
    onBack: () -> Unit,
    onOpenAttachment: (Notice) -> Unit,
    viewModel: NoticeDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val downloaded by viewModel.downloaded.collectAsStateWithLifecycle()

    ScreenScaffold("Notice", onBack = onBack) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            when (val s = state) {
                UiState.Loading -> LoadingView()
                is UiState.Error -> ErrorView(s.message, onRetry = viewModel::load, offline = s.offline)
                is UiState.Success -> NoticeDetailContent(s.data, downloaded, onOpenAttachment)
            }
        }
    }
}

@Composable
private fun NoticeDetailContent(
    notice: Notice,
    downloaded: Boolean,
    onOpenAttachment: (Notice) -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Published ${notice.publishedAt.toDateDisplay()}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        notice.expiresAt?.let { expiresAt ->
            Text(
                text = "Valid until ${expiresAt.toDateDisplay()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        notice.body?.let { body ->
            SelectionContainer {
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        notice.linkUrl?.takeIf { it.isSafeWebUrl() }?.let { url ->
            Button(
                onClick = { context.openInCustomTab(url) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open link")
            }
        }

        notice.attachment?.let { attachment ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Outlined.AttachFile,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${attachment.fileName} (${attachment.fileSize.toFileSize()})",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (downloaded) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Outlined.DownloadDone,
                                contentDescription = "Downloaded",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Downloaded",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Button(
                    onClick = { onOpenAttachment(notice) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open attachment")
                }
            }
        }
    }
}
