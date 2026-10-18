package com.rkgit.one.ui.navigation

import com.rkgit.one.data.files.FileKind
import com.rkgit.one.domain.model.Notice
import com.rkgit.one.domain.model.StudyMaterial

fun StudyMaterial.toViewerRoute(subjectName: String, year: Int, semester: Int): ViewerRoute = ViewerRoute(
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

fun Notice.toViewerRoute(): ViewerRoute? = attachment?.let {
    ViewerRoute(
        kind = FileKind.NOTICE.value,
        itemId = id,
        title = title,
        fileName = it.fileName,
        fileType = it.fileType,
        fileSize = it.fileSize,
        remotePath = it.path,
        subtitle = category.label
    )
}
