package com.rkgit.one.data.search

import com.rkgit.one.data.local.DownloadedFile

const val MIN_QUERY_LENGTH = 2
const val MAX_QUERY_LENGTH = 100
const val SEARCH_DEBOUNCE_MS = 350L
const val SEARCH_PAGE_SIZE = 20

fun normalizeQuery(raw: String?): String {
    if (raw == null) return ""
    val trimmed = raw.trim()
    val collapsed = trimmed.replace(Regex("\\s+"), " ")
    val cut = collapsed.take(MAX_QUERY_LENGTH)
    return cut.trim()
}

fun isSearchable(normalized: String): Boolean = normalized.length >= MIN_QUERY_LENGTH

fun searchDownloads(rows: List<DownloadedFile>, query: String): List<DownloadedFile> {
    val normalized = normalizeQuery(query)
    if (!isSearchable(normalized)) return emptyList()
    return rows
        .filter { row ->
            row.title.contains(normalized, ignoreCase = true) ||
                row.subtitle.contains(normalized, ignoreCase = true) ||
                row.fileName.contains(normalized, ignoreCase = true)
        }
        .sortedByDescending { it.downloadedAt }
}
