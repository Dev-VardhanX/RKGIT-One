package com.rkgit.one.domain.model

import java.time.Instant

data class StudyMaterial(
    val id: String,
    val subjectId: String,
    val title: String,
    val description: String?,
    val fileName: String,
    val filePath: String,
    val fileType: String,
    val fileSize: Long,
    val createdAt: Instant,
    val type: MaterialType = MaterialType.NOTES
) {
    val typeLabel: String
        get() = fileName.substringAfterLast('.', "").uppercase()
}
