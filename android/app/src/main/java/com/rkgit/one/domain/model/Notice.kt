package com.rkgit.one.domain.model

import java.time.Instant

enum class NoticeCategory(val value: String, val label: String) {
    GENERAL("general", "General"),
    ACADEMIC("academic", "Academic"),
    EXAM("exam", "Exam"),
    DATESHEET("datesheet", "Datesheet"),
    HOLIDAY("holiday", "Holiday");

    companion object {
        fun fromValue(v: String): NoticeCategory = entries.firstOrNull { it.value == v } ?: GENERAL
    }
}

data class NoticeAttachment(
    val path: String,
    val fileName: String,
    val fileType: String,
    val fileSize: Long
) {
    val isPdf: Boolean get() = fileType == "application/pdf"
    val isImage: Boolean get() = fileType.startsWith("image/")
    val typeLabel: String get() = if (isImage) "Image" else fileName.substringAfterLast('.', "").uppercase()
}

data class Notice(
    val id: String,
    val title: String,
    val body: String?,
    val category: NoticeCategory,
    val pinned: Boolean,
    val linkUrl: String?,
    val attachment: NoticeAttachment?,
    val publishedAt: Instant,
    val expiresAt: Instant?
)

fun List<Notice>.activeAt(now: Instant): List<Notice> = filter { it.expiresAt == null || it.expiresAt.isAfter(now) }
