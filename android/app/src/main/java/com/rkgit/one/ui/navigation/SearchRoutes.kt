package com.rkgit.one.ui.navigation

import com.rkgit.one.data.files.FileKind
import com.rkgit.one.data.local.DownloadedFile
import com.rkgit.one.domain.model.SearchMaterial

fun SearchMaterial.toViewerRoute(): ViewerRoute = ViewerRoute(
    kind = FileKind.MATERIAL.value,
    itemId = id,
    title = title,
    fileName = fileName,
    fileType = fileType,
    fileSize = fileSize,
    remotePath = filePath,
    subtitle = subjectName,
    year = year,
    semester = semester
)

fun DownloadedFile.toViewerRoute(): ViewerRoute = ViewerRoute(
    kind = kind,
    itemId = itemId,
    title = title,
    fileName = fileName,
    fileType = fileType,
    fileSize = sizeBytes,
    remotePath = remoteFilePath,
    subtitle = subtitle,
    year = year ?: 0,
    semester = semester ?: 0
)
