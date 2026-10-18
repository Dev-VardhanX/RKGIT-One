package com.rkgit.one

import com.rkgit.one.data.remote.dto.NoticeDto
import com.rkgit.one.data.remote.dto.toDomain
import com.rkgit.one.domain.model.Notice
import com.rkgit.one.domain.model.NoticeAttachment
import com.rkgit.one.domain.model.NoticeCategory
import com.rkgit.one.domain.model.activeAt
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.util.Locale

class NoticesMappingTest {

    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var savedLocale: Locale

    @Before
    fun setUp() {
        savedLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @After
    fun tearDown() {
        Locale.setDefault(savedLocale)
    }

    // --- NoticeDto attachment mapping ---

    @Test
    fun noticeDto_withAllFourFileFields_givesAttachment() {
        val dto = makeDto(
            filePath = "notices/doc.pdf",
            fileName = "doc.pdf",
            fileType = "application/pdf",
            fileSize = 1024L
        )
        val notice = dto.toDomain()
        assertNotNull(notice.attachment)
        assertEquals("notices/doc.pdf", notice.attachment?.path)
        assertEquals("doc.pdf", notice.attachment?.fileName)
        assertEquals("application/pdf", notice.attachment?.fileType)
        assertEquals(1024L, notice.attachment?.fileSize)
    }

    @Test
    fun noticeDto_withOnlyFilePath_givesNullAttachment() {
        val dto = makeDto(
            filePath = "notices/doc.pdf",
            fileName = null,
            fileType = null,
            fileSize = null
        )
        assertNull(dto.toDomain().attachment)
    }

    @Test
    fun noticeDto_withNoFileFields_givesNullAttachment() {
        val dto = makeDto(
            filePath = null,
            fileName = null,
            fileType = null,
            fileSize = null
        )
        assertNull(dto.toDomain().attachment)
    }

    @Test
    fun noticeDto_withBlankFilePath_givesNullAttachment() {
        val dto = makeDto(
            filePath = "   ",
            fileName = "doc.pdf",
            fileType = "application/pdf",
            fileSize = 1024L
        )
        assertNull(dto.toDomain().attachment)
    }

    // --- Category mapping ---

    @Test
    fun category_holiday_mapsToHoliday() {
        val dto = makeDto(category = "holiday")
        assertEquals(NoticeCategory.HOLIDAY, dto.toDomain().category)
    }

    @Test
    fun category_unknown_mapsToGeneral() {
        val dto = makeDto(category = "weird")
        assertEquals(NoticeCategory.GENERAL, dto.toDomain().category)
    }

    // --- String & Timestamp parsing ---

    @Test
    fun blankBody_mapsToNull() {
        val dto = makeDto(body = "   ")
        assertNull(dto.toDomain().body)
    }

    @Test
    fun blankLinkUrl_mapsToNull() {
        val dto = makeDto(linkUrl = "   ")
        assertNull(dto.toDomain().linkUrl)
    }

    @Test
    fun timestamps_withMicroseconds_parseCorrectly() {
        val dto = makeDto(
            publishedAt = "2026-10-18T10:00:00.123456+00:00",
            expiresAt = "2026-10-25T15:30:00.654321+00:00"
        )
        val notice = dto.toDomain()
        assertEquals("2026-10-18T10:00:00.123456Z", notice.publishedAt.toString())
        assertEquals("2026-10-25T15:30:00.654321Z", notice.expiresAt.toString())
    }

    // --- Attachment helpers ---

    @Test
    fun attachment_isPdf() {
        val attachment = NoticeAttachment("path", "file.pdf", "application/pdf", 1024L)
        assertTrue(attachment.isPdf)
        assertFalse(attachment.isImage)
    }

    @Test
    fun attachment_isImage() {
        val attachment = NoticeAttachment("path", "photo.png", "image/png", 2048L)
        assertTrue(attachment.isImage)
        assertFalse(attachment.isPdf)
    }

    @Test
    fun typeLabel_pdf() {
        val attachment = NoticeAttachment("path", "datesheet.pdf", "application/pdf", 1024L)
        assertEquals("PDF", attachment.typeLabel)
    }

    @Test
    fun typeLabel_image() {
        val attachment = NoticeAttachment("path", "scan.jpg", "image/jpeg", 1024L)
        assertEquals("Image", attachment.typeLabel)
    }

    // --- activeAt filtering ---

    @Test
    fun activeAt_filtersCorrectly() {
        val now = Instant.parse("2026-10-18T12:00:00Z")
        val nNull = makeNotice(id = "1", expiresAt = null)
        val nFuture = makeNotice(id = "2", expiresAt = Instant.parse("2026-10-19T12:00:00Z"))
        val nPast = makeNotice(id = "3", expiresAt = Instant.parse("2026-10-17T12:00:00Z"))
        val nEqual = makeNotice(id = "4", expiresAt = now)

        val list = listOf(nNull, nFuture, nPast, nEqual).activeAt(now)
        assertEquals(listOf(nNull, nFuture), list)
    }

    // --- Helpers ---

    private fun makeDto(
        id: String = "id-1",
        title: String = "Test Notice",
        body: String? = "Test Body",
        category: String = "general",
        pinned: Boolean = false,
        linkUrl: String? = null,
        filePath: String? = null,
        fileName: String? = null,
        fileType: String? = null,
        fileSize: Long? = null,
        publishedAt: String = "2026-10-18T10:00:00+00:00",
        expiresAt: String? = null
    ) = NoticeDto(
        id = id,
        title = title,
        body = body,
        category = category,
        pinned = pinned,
        linkUrl = linkUrl,
        filePath = filePath,
        fileName = fileName,
        fileType = fileType,
        fileSize = fileSize,
        publishedAt = publishedAt,
        expiresAt = expiresAt
    )

    private fun makeNotice(
        id: String,
        expiresAt: Instant?
    ) = Notice(
        id = id,
        title = "Notice $id",
        body = "Body",
        category = NoticeCategory.GENERAL,
        pinned = false,
        linkUrl = null,
        attachment = null,
        publishedAt = Instant.parse("2026-10-18T00:00:00Z"),
        expiresAt = expiresAt
    )
}
