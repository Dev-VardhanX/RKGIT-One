package com.rkgit.one

import com.rkgit.one.data.remote.dto.RecentMaterialDto
import com.rkgit.one.data.remote.dto.toDomain
import com.rkgit.one.domain.model.Event
import com.rkgit.one.domain.model.Notice
import com.rkgit.one.domain.model.NoticeCategory
import com.rkgit.one.domain.model.RecentMaterial
import com.rkgit.one.domain.model.StudyMaterial
import com.rkgit.one.ui.screens.home.HomeData
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.util.Locale

class HomeMappingTest {

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

    @Test
    fun recentMaterialDto_decodesAndMapsCorrectly() {
        val rawJson = """
            {
                "id": "mat-1",
                "subject_id": "sub-1",
                "title": "Calculus Notes",
                "description": "   ",
                "file_path": "materials/notes.pdf",
                "file_name": "notes.pdf",
                "file_type": "application/pdf",
                "file_size": 2048,
                "created_at": "2026-10-18T10:00:00.123456+00:00",
                "subjects": {
                    "name": "Maths",
                    "year": 1,
                    "semester": 2
                }
            }
        """.trimIndent()

        val dto = json.decodeFromString<RecentMaterialDto>(rawJson)
        val domain = dto.toDomain()

        assertEquals("mat-1", domain.material.id)
        assertEquals("sub-1", domain.material.subjectId)
        assertEquals("Calculus Notes", domain.material.title)
        assertNull(domain.material.description)
        assertEquals("notes.pdf", domain.material.fileName)
        assertEquals("materials/notes.pdf", domain.material.filePath)
        assertEquals("application/pdf", domain.material.fileType)
        assertEquals(2048L, domain.material.fileSize)
        assertEquals("2026-10-18T10:00:00.123456Z", domain.material.createdAt.toString())
        assertEquals("Maths", domain.subjectName)
        assertEquals(1, domain.year)
        assertEquals(2, domain.semester)
    }

    @Test
    fun recentMaterialDto_withNullDescription_mapsToNull() {
        val rawJson = """
            {
                "id": "mat-2",
                "subject_id": "sub-2",
                "title": "Physics Notes",
                "description": null,
                "file_path": "materials/phys.pdf",
                "file_name": "phys.pdf",
                "file_type": "application/pdf",
                "file_size": 1024,
                "created_at": "2026-10-18T11:00:00.000000+00:00",
                "subjects": {
                    "name": "Physics",
                    "year": 1,
                    "semester": 1
                }
            }
        """.trimIndent()

        val dto = json.decodeFromString<RecentMaterialDto>(rawJson)
        val domain = dto.toDomain()

        assertNull(domain.material.description)
        assertEquals("Physics", domain.subjectName)
    }

    @Test
    fun homeData_isEmpty_trueWhenAllEmpty() {
        val data = HomeData(notices = emptyList(), materials = emptyList(), events = emptyList())
        assertTrue(data.isEmpty)
    }

    @Test
    fun homeData_isEmpty_falseWhenNoticesPresent() {
        val notice = Notice(
            id = "n1",
            title = "Notice 1",
            body = null,
            category = NoticeCategory.GENERAL,
            pinned = false,
            linkUrl = null,
            attachment = null,
            publishedAt = Instant.now(),
            expiresAt = null
        )
        val data = HomeData(notices = listOf(notice), materials = emptyList(), events = emptyList())
        assertFalse(data.isEmpty)
    }

    @Test
    fun homeData_isEmpty_falseWhenMaterialsPresent() {
        val material = RecentMaterial(
            material = StudyMaterial(
                id = "m1",
                subjectId = "s1",
                title = "Mat 1",
                description = null,
                fileName = "f.pdf",
                filePath = "p",
                fileType = "application/pdf",
                fileSize = 100L,
                createdAt = Instant.now()
            ),
            subjectName = "Sub",
            year = 1,
            semester = 1
        )
        val data = HomeData(notices = emptyList(), materials = listOf(material), events = emptyList())
        assertFalse(data.isEmpty)
    }

    @Test
    fun homeData_isEmpty_falseWhenEventsPresent() {
        val event = Event(
            id = "e1",
            title = "Event 1",
            description = null,
            eventDate = Instant.now(),
            venue = null,
            posterUrl = null,
            registrationUrl = null
        )
        val data = HomeData(notices = emptyList(), materials = emptyList(), events = listOf(event))
        assertFalse(data.isEmpty)
    }
}
