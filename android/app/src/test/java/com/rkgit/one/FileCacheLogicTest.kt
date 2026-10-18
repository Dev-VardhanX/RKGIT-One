package com.rkgit.one

import com.rkgit.one.data.files.CachedFileInfo
import com.rkgit.one.data.files.cacheFileName
import com.rkgit.one.data.files.downloadProgress
import com.rkgit.one.data.files.filesToTrim
import com.rkgit.one.data.files.isCompleteDownload
import com.rkgit.one.data.files.resumeKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FileCacheLogicTest {

    @Test
    fun cacheFileName_sameInputsGiveSameName() {
        val name1 = cacheFileName("mat-1", "semester1/math.pdf", "math.pdf")
        val name2 = cacheFileName("mat-1", "semester1/math.pdf", "math.pdf")
        assertEquals(name1, name2)
    }

    @Test
    fun cacheFileName_differentRemotePathGivesDifferentName() {
        val name1 = cacheFileName("mat-1", "pathA/file.pdf", "file.pdf")
        val name2 = cacheFileName("mat-1", "pathB/file.pdf", "file.pdf")
        assertNotEquals(name1, name2)
    }

    @Test
    fun cacheFileName_extensionPreservedInLowercase() {
        val name = cacheFileName("item1", "path/doc", "Notes.PDF")
        assertTrue(name.endsWith(".pdf"))
    }

    @Test
    fun cacheFileName_noExtensionDefaultsToBin() {
        val name = cacheFileName("item1", "path/noext", "noextension")
        assertTrue(name.endsWith(".bin"))
    }

    @Test
    fun cacheFileName_unsafeCharactersInItemIdAreRemoved() {
        val name1 = cacheFileName("item/1!@#$%^&*()_+={}", "path/file.pdf", "file.pdf")
        assertTrue(name1.startsWith("item1_"))

        val name2 = cacheFileName("item/1-test!@#$", "path/file.pdf", "file.pdf")
        assertTrue(name2.startsWith("item1-test_"))
    }

    @Test
    fun cacheFileName_containsNoSlashOrSpaces() {
        val name = cacheFileName("item 1 / test", "path with spaces/file name.pdf", "file name.pdf")
        assertFalse(name.contains("/"))
        assertFalse(name.contains(" "))
        assertFalse(name.contains("\\"))
    }

    @Test
    fun filesToTrim_withinLimitReturnsEmpty() {
        val files = listOf(
            CachedFileInfo("f1", 100L, 1000L),
            CachedFileInfo("f2", 200L, 2000L)
        )
        val toTrim = filesToTrim(files, 500L)
        assertTrue(toTrim.isEmpty())
    }

    @Test
    fun filesToTrim_overLimitDeletesOldestFirstUntilFits() {
        val f1 = CachedFileInfo("oldest", 100L, 1000L)
        val f2 = CachedFileInfo("second", 150L, 2000L)
        val f3 = CachedFileInfo("third", 200L, 3000L)
        val f4 = CachedFileInfo("newest", 250L, 4000L)
        // Total = 700L. Limit = 400L.
        // Oldest f1 (100L) removed -> remaining 600L (> 400)
        // Second f2 (150L) removed -> remaining 450L (> 400)
        // Third f3 (200L) removed -> remaining 250L (<= 400) -> stops!
        val files = listOf(f3, f1, f4, f2) // shuffled order
        val toTrim = filesToTrim(files, 400L)

        assertEquals(listOf(f1, f2, f3), toTrim)
        assertEquals(listOf("oldest", "second", "third"), toTrim.map { it.name })
    }

    @Test
    fun filesToTrim_zeroLimitDeletesAllFiles() {
        val f1 = CachedFileInfo("f1", 100L, 1000L)
        val f2 = CachedFileInfo("f2", 200L, 2000L)
        val files = listOf(f2, f1)
        val toTrim = filesToTrim(files, 0L)
        assertEquals(listOf(f1, f2), toTrim)
    }

    @Test
    fun downloadProgress_calculations() {
        assertNull(downloadProgress(10, 0))
        assertNull(downloadProgress(10, -5))
        assertEquals(0.5f, downloadProgress(50, 100)!!, 0.001f)
        assertEquals(1f, downloadProgress(120, 100)!!, 0.001f)
        assertEquals(0f, downloadProgress(0, 100)!!, 0.001f)
    }

    @Test
    fun isCompleteDownload_checks() {
        assertTrue(isCompleteDownload(100L, 100L))
        assertFalse(isCompleteDownload(100L, 50L))
        assertTrue(isCompleteDownload(0L, 50L))
        assertTrue(isCompleteDownload(-1L, 50L))
    }

    @Test
    fun resumeKey_formatsCorrectly() {
        assertEquals("material:abc", resumeKey("material", "abc"))
        assertEquals("notice:123", resumeKey("notice", "123"))
    }
}
