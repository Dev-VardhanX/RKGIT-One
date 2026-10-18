package com.rkgit.one.ui.screens.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rkgit.one.data.downloads.GroupedDownloads
import com.rkgit.one.data.downloads.RemoteFileInfo
import com.rkgit.one.data.downloads.RemoteStatus
import com.rkgit.one.data.downloads.deriveRemoteStatus
import com.rkgit.one.data.downloads.downloadKey
import com.rkgit.one.data.downloads.groupDownloads
import com.rkgit.one.data.downloads.totalSizeBytes
import com.rkgit.one.data.downloads.DownloadRepository
import com.rkgit.one.data.files.FileKind
import com.rkgit.one.data.local.DownloadedFile
import com.rkgit.one.data.repository.RemoteFilesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DownloadsUi(
    val groups: GroupedDownloads = GroupedDownloads(emptyList(), emptyList()),
    val statuses: Map<String, RemoteStatus> = emptyMap(),
    val remote: Map<String, RemoteFileInfo> = emptyMap(),
    val checkFailed: Boolean = false,
    val refreshing: Boolean = false,
    val count: Int = 0,
    val totalBytes: Long = 0L
)

private data class RemoteCheckState(
    val checked: Boolean = false,
    val failed: Boolean = false,
    val remote: Map<String, RemoteFileInfo> = emptyMap(),
    val refreshing: Boolean = false
)

@OptIn(FlowPreview::class)
@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository,
    private val remoteFilesRepository: RemoteFilesRepository
) : ViewModel() {

    val rows: Flow<List<DownloadedFile>> = downloadRepository.observeAll()

    private val _remoteState = MutableStateFlow(RemoteCheckState())

    val uiState: StateFlow<DownloadsUi> = combine(rows, _remoteState) { currentRows, remoteState ->
        val groups = groupDownloads(currentRows)
        val statuses = currentRows.associate { row ->
            val key = downloadKey(row.kind, row.itemId)
            key to deriveRemoteStatus(row.remoteFilePath, remoteState.remote[key], remoteState.checked)
        }
        DownloadsUi(
            groups = groups,
            statuses = statuses,
            remote = remoteState.remote,
            checkFailed = remoteState.failed,
            refreshing = remoteState.refreshing,
            count = currentRows.size,
            totalBytes = currentRows.totalSizeBytes()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DownloadsUi()
    )

    init {
        viewModelScope.launch {
            rows.map { list -> list.map { downloadKey(it.kind, it.itemId) }.toSet() }
                .distinctUntilChanged()
                .debounce(500)
                .collectLatest { keys ->
                    if (keys.isNotEmpty()) {
                        val current = rows.first()
                        performCheck(current)
                    } else {
                        _remoteState.update {
                            it.copy(checked = false, failed = false, remote = emptyMap())
                        }
                    }
                }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val current = rows.first()
            performCheck(current, isRefresh = true)
        }
    }

    private suspend fun performCheck(currentRows: List<DownloadedFile>, isRefresh: Boolean = false) {
        if (currentRows.isEmpty()) {
            _remoteState.update {
                it.copy(checked = true, failed = false, remote = emptyMap(), refreshing = false)
            }
            return
        }
        if (isRefresh) {
            _remoteState.update { it.copy(refreshing = true) }
        }
        try {
            val result = remoteFilesRepository.check(currentRows)
            _remoteState.update {
                it.copy(
                    checked = true,
                    failed = false,
                    remote = result,
                    refreshing = false
                )
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            _remoteState.update {
                it.copy(
                    checked = false,
                    failed = true,
                    refreshing = false
                )
            }
        }
    }

    fun remove(row: DownloadedFile) {
        viewModelScope.launch {
            downloadRepository.remove(FileKind.fromValue(row.kind), row.itemId)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            downloadRepository.removeAll()
        }
    }

    fun update(row: DownloadedFile, info: RemoteFileInfo) {
        downloadRepository.enqueue(
            kind = FileKind.fromValue(row.kind),
            itemId = row.itemId,
            title = info.title,
            subtitle = row.subtitle,
            year = row.year,
            semester = row.semester,
            fileName = info.fileName,
            fileType = info.fileType,
            sizeBytes = info.fileSize,
            remotePath = info.filePath,
            isUpdate = true
        )
    }
}
