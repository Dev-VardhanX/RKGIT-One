package com.rkgit.one.data.downloads

import com.rkgit.one.data.local.DownloadedFile

data class SemesterGroup(
    val year: Int?,
    val semester: Int?,
    val items: List<DownloadedFile>
)

data class GroupedDownloads(
    val notes: List<SemesterGroup>,
    val notices: List<DownloadedFile>
)

fun groupDownloads(rows: List<DownloadedFile>): GroupedDownloads {
    if (rows.isEmpty()) {
        return GroupedDownloads(emptyList(), emptyList())
    }

    val itemComparator = compareBy<DownloadedFile, String>(String.CASE_INSENSITIVE_ORDER) { it.subtitle }
        .thenByDescending { it.downloadedAt }

    val materialRows = rows.filter { it.kind == "material" }
    val noticeRows = rows.filter { it.kind == "notice" }

    val validGroups = mutableMapOf<Pair<Int, Int>, MutableList<DownloadedFile>>()
    val otherNotes = mutableListOf<DownloadedFile>()

    for (row in materialRows) {
        val y = row.year
        val s = row.semester
        if (y != null && s != null && y > 0 && s > 0) {
            validGroups.getOrPut(Pair(y, s)) { mutableListOf() }.add(row)
        } else {
            otherNotes.add(row)
        }
    }

    val sortedSemesterGroups = validGroups.entries
        .sortedWith(compareBy({ it.key.first }, { it.key.second }))
        .map { (key, items) ->
            SemesterGroup(
                year = key.first,
                semester = key.second,
                items = items.sortedWith(itemComparator)
            )
        }
        .toMutableList()

    if (otherNotes.isNotEmpty()) {
        sortedSemesterGroups.add(
            SemesterGroup(
                year = null,
                semester = null,
                items = otherNotes.sortedWith(itemComparator)
            )
        )
    }

    val sortedNotices = noticeRows.sortedByDescending { it.downloadedAt }

    return GroupedDownloads(
        notes = sortedSemesterGroups,
        notices = sortedNotices
    )
}

fun List<DownloadedFile>.totalSizeBytes(): Long = sumOf { it.sizeBytes }
