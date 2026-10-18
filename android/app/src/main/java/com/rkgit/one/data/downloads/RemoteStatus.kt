package com.rkgit.one.data.downloads

enum class RemoteStatus {
    UP_TO_DATE,
    UPDATE_AVAILABLE,
    UNAVAILABLE,
    UNKNOWN
}

data class RemoteFileInfo(
    val title: String,
    val fileName: String,
    val fileType: String,
    val fileSize: Long,
    val filePath: String
)

fun deriveRemoteStatus(
    localRemotePath: String,
    remote: RemoteFileInfo?,
    checked: Boolean
): RemoteStatus {
    return when {
        !checked -> RemoteStatus.UNKNOWN
        remote == null -> RemoteStatus.UNAVAILABLE
        remote.filePath != localRemotePath -> RemoteStatus.UPDATE_AVAILABLE
        else -> RemoteStatus.UP_TO_DATE
    }
}
