package com.rkgit.one.data.downloads

sealed interface DownloadState {
    data object NotDownloaded : DownloadState
    data class Downloading(val progress: Float?) : DownloadState
    data class Downloaded(val localPath: String) : DownloadState
    data object UpdateAvailable : DownloadState
    data class Failed(val reason: String) : DownloadState
}

enum class WorkStatus {
    ENQUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    CANCELLED
}

data class WorkSnapshot(
    val status: WorkStatus,
    val progress: Float?,
    val error: String?
)

data class DownloadRow(
    val remoteFilePath: String,
    val localPath: String
)

fun deriveDownloadState(
    row: DownloadRow?,
    currentRemotePath: String?,
    work: WorkSnapshot?
): DownloadState {
    if (work?.status == WorkStatus.ENQUEUED || work?.status == WorkStatus.RUNNING) {
        return DownloadState.Downloading(work.progress)
    }
    if (row != null && (currentRemotePath == null || row.remoteFilePath == currentRemotePath)) {
        return DownloadState.Downloaded(row.localPath)
    }
    if (row != null) {
        return DownloadState.UpdateAvailable
    }
    if (work?.status == WorkStatus.FAILED) {
        return DownloadState.Failed(work.error ?: "Download failed")
    }
    return DownloadState.NotDownloaded
}
