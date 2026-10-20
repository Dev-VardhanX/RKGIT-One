package com.rkgit.one.domain.model

import java.time.Instant
import java.util.Locale

data class SearchMaterial(
    val id: String,
    val title: String,
    val subjectId: String,
    val subjectName: String,
    val year: Int,
    val semester: Int,
    val snippet: String?,
    val filePath: String,
    val fileName: String,
    val fileType: String,
    val fileSize: Long
) {
    val typeLabel: String
        get() {
            val ext = fileName.substringAfterLast('.', "")
            return if (ext.isNotBlank()) ext.uppercase(Locale.getDefault()) else "FILE"
        }
}

data class SearchEvent(
    val id: String,
    val title: String,
    val venue: String?,
    val snippet: String?,
    val eventDate: Instant
)

data class SearchNotice(
    val id: String,
    val title: String,
    val category: NoticeCategory,
    val pinned: Boolean,
    val hasAttachment: Boolean,
    val snippet: String?
)

data class SearchResults(
    val materials: List<SearchMaterial>,
    val events: List<SearchEvent>,
    val notices: List<SearchNotice>
) {
    val total: Int get() = materials.size + events.size + notices.size
    val isEmpty: Boolean get() = total == 0
}
