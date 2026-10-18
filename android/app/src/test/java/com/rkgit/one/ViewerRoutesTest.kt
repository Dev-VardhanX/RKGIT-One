package com.rkgit.one

import com.rkgit.one.data.files.ContentKind
import com.rkgit.one.data.files.contentKindOf
import com.rkgit.one.domain.model.Notice
import com.rkgit.one.domain.model.NoticeAttachment
import com.rkgit.one.domain.model.NoticeCategory
import com.rkgit.one.domain.model.StudyMaterial
import com.rkgit.one.ui.navigation.toViewerRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class ViewerRoutesTest {

    @Test
    fun contentKindOf_mapsPdfCorrectly() {
        assertEquals(ContentKind.PDF, contentKindOf("application/pdf"))
        assertEquals(ContentKind.PDF, contentKindOf("APPLICATION/PDF"))
    }

    @Test
    fun contentKindOf_mapsImagesCorrectly() {
        assertEquals(ContentKind.IMAGE, contentKindOf("image/png"))
        assertEquals(ContentKind.IMAGE, contentKindOf("image/jpeg"))
        assertEquals(ContentKind.IMAGE, contentKindOf("image/webp"))
    }

    @Test
    fun contentKindOf_mapsOtherCorrectly() {
        assertEquals(
            ContentKind.OTHER,
            contentKindOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
        )
        assertEquals(ContentKind.OTHER, contentKindOf(""))
    }

    @Test
    fun notice_withAttachment_givesViewerRoute() {
        val notice = Notice(
            id = "notice-123",
            title = "Test Notice Title",
            body = "Notice body text",
            category = NoticeCategory.ACADEMIC,
            pinned = false,
            linkUrl = null,
            attachment = NoticeAttachment(
                path = "notices/sample.png",
                fileName = "sample.png",
                fileType = "image/png",
                fileSize = 1024L
            ),
            publishedAt = Instant.parse("2026-10-18T10:00:00Z"),
            expiresAt = null
        )

        val route = notice.toViewerRoute()
        assertNotNull(route)
        assertEquals("notice", route?.kind)
        assertEquals("notice-123", route?.itemId)
        assertEquals("Test Notice Title", route?.title)
        assertEquals("sample.png", route?.fileName)
        assertEquals("image/png", route?.fileType)
        assertEquals(1024L, route?.fileSize)
        assertEquals("notices/sample.png", route?.remotePath)
        assertEquals("Academic", route?.subtitle)
        assertEquals(0, route?.year)
        assertEquals(0, route?.semester)
    }

    @Test
    fun notice_withoutAttachment_givesNull() {
        val notice = Notice(
            id = "notice-no-att",
            title = "No Attachment Notice",
            body = null,
            category = NoticeCategory.GENERAL,
            pinned = false,
            linkUrl = null,
            attachment = null,
            publishedAt = Instant.parse("2026-10-18T10:00:00Z"),
            expiresAt = null
        )

        val route = notice.toViewerRoute()
        assertNull(route)
    }

    @Test
    fun studyMaterial_toViewerRoute_copiesAllFields() {
        val material = StudyMaterial(
            id = "mat-456",
            subjectId = "sub-789",
            title = "Math Lecture Notes",
            description = "Week 1 notes",
            fileName = "math1.pdf",
            filePath = "semester1/math1.pdf",
            fileType = "application/pdf",
            fileSize = 204800L,
            createdAt = Instant.parse("2026-10-18T10:00:00Z")
        )

        val route = material.toViewerRoute(subjectName = "Mathematics", year = 1, semester = 1)
        assertEquals("material", route.kind)
        assertEquals("mat-456", route.itemId)
        assertEquals("Math Lecture Notes", route.title)
        assertEquals("math1.pdf", route.fileName)
        assertEquals("application/pdf", route.fileType)
        assertEquals(204800L, route.fileSize)
        assertEquals("semester1/math1.pdf", route.remotePath)
        assertEquals("Mathematics", route.subtitle)
        assertEquals(1, route.year)
        assertEquals(1, route.semester)
    }
}
