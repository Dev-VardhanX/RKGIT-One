package com.rkgit.one.data.local

import androidx.room.Entity

@Entity(tableName = "downloads", primaryKeys = ["kind", "itemId"])
data class DownloadedFile(
    val kind: String,
    val itemId: String,
    val title: String,
    val subtitle: String,
    val year: Int?,
    val semester: Int?,
    val fileName: String,
    val fileType: String,
    val sizeBytes: Long,
    val remoteFilePath: String,
    val localPath: String,
    val downloadedAt: Long
)
