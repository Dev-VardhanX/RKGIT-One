package com.rkgit.one

import com.rkgit.one.data.downloads.DownloadRow
import com.rkgit.one.data.downloads.DownloadState
import com.rkgit.one.data.downloads.MOBILE_DATA_WARNING_BYTES
import com.rkgit.one.data.downloads.WorkSnapshot
import com.rkgit.one.data.downloads.WorkStatus
import com.rkgit.one.data.downloads.deriveDownloadState
import com.rkgit.one.data.downloads.downloadKey
import com.rkgit.one.data.downloads.shouldConfirmMobileData
import com.rkgit.one.data.downloads.uniqueWorkName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadLogicTest {

    @Test
    fun formats_downloadKey_and_uniqueWorkName() {
        assertEquals("material:123", downloadKey("material", "123"))
        assertEquals("notice:abc-def", downloadKey("notice", "abc-def"))

        assertEquals("download:material:123", uniqueWorkName("material", "123"))
        assertEquals("download:notice:abc-def", uniqueWorkName("notice", "abc-def"))
    }

    @Test
    fun shouldConfirmMobileData_checks() {
        // metered and size == threshold -> true
        assertTrue(shouldConfirmMobileData(MOBILE_DATA_WARNING_BYTES, isMetered = true))

        // metered and size below -> false
        assertFalse(shouldConfirmMobileData(MOBILE_DATA_WARNING_BYTES - 1L, isMetered = true))

        // not metered and huge -> false
        assertFalse(shouldConfirmMobileData(100L * 1024 * 1024, isMetered = false))

        // metered and 0 -> false
        assertFalse(shouldConfirmMobileData(0L, isMetered = true))
    }

    @Test
    fun deriveDownloadState_no_row_and_no_work() {
        val state = deriveDownloadState(
            row = null,
            currentRemotePath = "path/file.pdf",
            work = null
        )
        assertEquals(DownloadState.NotDownloaded, state)
    }

    @Test
    fun deriveDownloadState_no_row_and_RUNNING_with_progress() {
        val state = deriveDownloadState(
            row = null,
            currentRemotePath = "path/file.pdf",
            work = WorkSnapshot(WorkStatus.RUNNING, 0.4f, null)
        )
        assertEquals(DownloadState.Downloading(0.4f), state)
    }

    @Test
    fun deriveDownloadState_no_row_and_ENQUEUED() {
        val state = deriveDownloadState(
            row = null,
            currentRemotePath = "path/file.pdf",
            work = WorkSnapshot(WorkStatus.ENQUEUED, null, null)
        )
        assertEquals(DownloadState.Downloading(null), state)
    }

    @Test
    fun deriveDownloadState_row_with_same_remote_path_and_no_work() {
        val state = deriveDownloadState(
            row = DownloadRow("path/file.pdf", "/files/downloads/file.pdf"),
            currentRemotePath = "path/file.pdf",
            work = null
        )
        assertEquals(DownloadState.Downloaded("/files/downloads/file.pdf"), state)
    }

    @Test
    fun deriveDownloadState_row_with_different_remote_path_and_no_work() {
        val state = deriveDownloadState(
            row = DownloadRow("path/old_file.pdf", "/files/downloads/old_file.pdf"),
            currentRemotePath = "path/new_file.pdf",
            work = null
        )
        assertEquals(DownloadState.UpdateAvailable, state)
    }

    @Test
    fun deriveDownloadState_row_with_different_path_and_RUNNING() {
        val state = deriveDownloadState(
            row = DownloadRow("path/old_file.pdf", "/files/downloads/old_file.pdf"),
            currentRemotePath = "path/new_file.pdf",
            work = WorkSnapshot(WorkStatus.RUNNING, 0.25f, null)
        )
        assertEquals(DownloadState.Downloading(0.25f), state)
    }

    @Test
    fun deriveDownloadState_row_with_same_path_and_FAILED_work() {
        val state = deriveDownloadState(
            row = DownloadRow("path/file.pdf", "/files/downloads/file.pdf"),
            currentRemotePath = "path/file.pdf",
            work = WorkSnapshot(WorkStatus.FAILED, null, "Network error")
        )
        assertEquals(DownloadState.Downloaded("/files/downloads/file.pdf"), state)
    }

    @Test
    fun deriveDownloadState_no_row_and_FAILED_with_error() {
        val state = deriveDownloadState(
            row = null,
            currentRemotePath = "path/file.pdf",
            work = WorkSnapshot(WorkStatus.FAILED, null, "x")
        )
        assertEquals(DownloadState.Failed("x"), state)
    }

    @Test
    fun deriveDownloadState_no_row_and_FAILED_with_null_error() {
        val state = deriveDownloadState(
            row = null,
            currentRemotePath = "path/file.pdf",
            work = WorkSnapshot(WorkStatus.FAILED, null, null)
        )
        assertEquals(DownloadState.Failed("Download failed"), state)
    }

    @Test
    fun deriveDownloadState_no_row_and_SUCCEEDED_or_CANCELLED() {
        val state1 = deriveDownloadState(
            row = null,
            currentRemotePath = "path/file.pdf",
            work = WorkSnapshot(WorkStatus.SUCCEEDED, null, null)
        )
        assertEquals(DownloadState.NotDownloaded, state1)

        val state2 = deriveDownloadState(
            row = null,
            currentRemotePath = "path/file.pdf",
            work = WorkSnapshot(WorkStatus.CANCELLED, null, null)
        )
        assertEquals(DownloadState.NotDownloaded, state2)
    }

    @Test
    fun deriveDownloadState_currentRemotePath_null_with_row() {
        val state = deriveDownloadState(
            row = DownloadRow("path/file.pdf", "/files/downloads/file.pdf"),
            currentRemotePath = null,
            work = null
        )
        assertEquals(DownloadState.Downloaded("/files/downloads/file.pdf"), state)
    }
}
