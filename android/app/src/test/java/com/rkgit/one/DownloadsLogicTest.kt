package com.rkgit.one

import com.rkgit.one.data.downloads.RemoteFileInfo
import com.rkgit.one.data.downloads.RemoteStatus
import com.rkgit.one.data.downloads.SemesterGroup
import com.rkgit.one.data.downloads.deriveRemoteStatus
import com.rkgit.one.data.downloads.groupDownloads
import com.rkgit.one.data.downloads.totalSizeBytes
import com.rkgit.one.data.local.DownloadedFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadsLogicTest {

    private fun sampleFile(
        kind: String = "material",
        itemId: String,
        title: String = "Title",
        subtitle: String = "Subject",
        year: Int? = 1,
        semester: Int? = 1,
        fileName: String = "file.pdf",
        fileType: String = "application/pdf",
        sizeBytes: Long = 1000L,
        remoteFilePath: String = "path/file.pdf",
        localPath: String = "/data/local/path/file.pdf",
        downloadedAt: Long = 1000L
    ) = DownloadedFile(
        kind = kind,
        itemId = itemId,
        title = title,
        subtitle = subtitle,
        year = year,
        semester = semester,
        fileName = fileName,
        fileType = fileType,
        sizeBytes = sizeBytes,
        remoteFilePath = remoteFilePath,
        localPath = localPath,
        downloadedAt = downloadedAt
    )

    @Test
    fun deriveRemoteStatus_unchecked_returnsUnknown() {
        assertEquals(
            RemoteStatus.UNKNOWN,
            deriveRemoteStatus("path/file.pdf", null, checked = false)
        )
        val remote = RemoteFileInfo("Title", "file.pdf", "application/pdf", 1000L, "path/file.pdf")
        assertEquals(
            RemoteStatus.UNKNOWN,
            deriveRemoteStatus("path/file.pdf", remote, checked = false)
        )
    }

    @Test
    fun deriveRemoteStatus_remoteNull_returnsUnavailable() {
        assertEquals(
            RemoteStatus.UNAVAILABLE,
            deriveRemoteStatus("path/file.pdf", null, checked = true)
        )
    }

    @Test
    fun deriveRemoteStatus_differentFilePath_returnsUpdateAvailable() {
        val remote = RemoteFileInfo("Title", "file.pdf", "application/pdf", 1000L, "path/new_version.pdf")
        assertEquals(
            RemoteStatus.UPDATE_AVAILABLE,
            deriveRemoteStatus("path/old_version.pdf", remote, checked = true)
        )
    }

    @Test
    fun deriveRemoteStatus_sameFilePath_returnsUpToDate() {
        val remote = RemoteFileInfo("Title", "file.pdf", "application/pdf", 1000L, "path/file.pdf")
        assertEquals(
            RemoteStatus.UP_TO_DATE,
            deriveRemoteStatus("path/file.pdf", remote, checked = true)
        )
    }

    @Test
    fun groupDownloads_emptyInput_returnsEmptyGroups() {
        val grouped = groupDownloads(emptyList())
        assertTrue(grouped.notes.isEmpty())
        assertTrue(grouped.notices.isEmpty())
        assertEquals(0L, emptyList<DownloadedFile>().totalSizeBytes())
    }

    @Test
    fun totalSizeBytes_sumsFileSizes() {
        val files = listOf(
            sampleFile(itemId = "1", sizeBytes = 100L),
            sampleFile(itemId = "2", sizeBytes = 250L),
            sampleFile(itemId = "3", sizeBytes = 350L)
        )
        assertEquals(700L, files.totalSizeBytes())
    }

    @Test
    fun groupDownloads_groupsAndSortsCorrectly() {
        // Mix:
        // year 1 / sem 2:
        val m1_2_physOld = sampleFile(
            itemId = "m-phys-old",
            subtitle = "Physics",
            year = 1,
            semester = 2,
            downloadedAt = 1000L
        )
        val m1_2_chem = sampleFile(
            itemId = "m-chem",
            subtitle = "chemistry", // lowercase 'c' test case-insensitive
            year = 1,
            semester = 2,
            downloadedAt = 2000L
        )
        val m1_2_physNew = sampleFile(
            itemId = "m-phys-new",
            subtitle = "Physics",
            year = 1,
            semester = 2,
            downloadedAt = 3000L // same subject as physOld, but downloaded later
        )

        // year 1 / sem 1:
        val m1_1 = sampleFile(
            itemId = "m-math",
            subtitle = "Math",
            year = 1,
            semester = 1,
            downloadedAt = 500L
        )

        // year 2 / sem 3:
        val m2_3 = sampleFile(
            itemId = "m-algo",
            subtitle = "Algorithms",
            year = 2,
            semester = 3,
            downloadedAt = 600L
        )

        // null year / sem:
        val m_null = sampleFile(
            itemId = "m-other",
            subtitle = "Other",
            year = null,
            semester = null,
            downloadedAt = 700L
        )

        // two notices:
        val n_old = sampleFile(
            kind = "notice",
            itemId = "n-old",
            title = "Old Notice",
            subtitle = "Academic",
            year = null,
            semester = null,
            downloadedAt = 1500L
        )
        val n_new = sampleFile(
            kind = "notice",
            itemId = "n-new",
            title = "New Notice",
            subtitle = "Datesheet",
            year = null,
            semester = null,
            downloadedAt = 2500L
        )

        val input = listOf(m1_2_physOld, n_old, m2_3, m1_2_chem, m_null, n_new, m1_1, m1_2_physNew)
        val grouped = groupDownloads(input)

        // Group count: 4 groups for notes (1/1, 1/2, 2/3, null)
        assertEquals(4, grouped.notes.size)

        // Order of groups: 1/1, 1/2, 2/3, then null group last
        val g1 = grouped.notes[0]
        assertEquals(1, g1.year)
        assertEquals(1, g1.semester)
        assertEquals(listOf("m-math"), g1.items.map { it.itemId })

        val g2 = grouped.notes[1]
        assertEquals(1, g2.year)
        assertEquals(2, g2.semester)
        // Inside group 1/2: "chemistry" comes before "Physics", and "m-phys-new" (3000L) before "m-phys-old" (1000L)
        assertEquals(listOf("m-chem", "m-phys-new", "m-phys-old"), g2.items.map { it.itemId })

        val g3 = grouped.notes[2]
        assertEquals(2, g3.year)
        assertEquals(3, g3.semester)
        assertEquals(listOf("m-algo"), g3.items.map { it.itemId })

        val g4 = grouped.notes[3]
        assertEquals(null, g4.year)
        assertEquals(null, g4.semester)
        assertEquals(listOf("m-other"), g4.items.map { it.itemId })

        // Notices: newest first, separated from notes
        assertEquals(2, grouped.notices.size)
        assertEquals(listOf("n-new", "n-old"), grouped.notices.map { it.itemId })
        assertTrue(grouped.notices.all { it.kind == "notice" })
        assertTrue(grouped.notes.all { group -> group.items.all { it.kind == "material" } })
    }
}
