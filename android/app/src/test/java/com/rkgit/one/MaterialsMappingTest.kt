package com.rkgit.one

import com.rkgit.one.data.remote.dto.CountDto
import com.rkgit.one.data.remote.dto.StudyMaterialDto
import com.rkgit.one.data.remote.dto.SubjectDto
import com.rkgit.one.data.remote.dto.toDomain
import com.rkgit.one.domain.model.StudyMaterial
import com.rkgit.one.ui.util.toFileSize
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.Locale

class MaterialsMappingTest {

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

    // --- SubjectDto ---

    @Test
    fun subjectDto_withMaterialCount_givesCorrectCount() {
        val dto = json.decodeFromString<SubjectDto>(
            """{"id":"a","year":1,"semester":1,"name":"Maths","study_materials":[{"count":3}]}"""
        )
        assertEquals(3, dto.toDomain().materialCount)
    }

    @Test
    fun subjectDto_withEmptyMaterialsList_givesZero() {
        val dto = json.decodeFromString<SubjectDto>(
            """{"id":"a","year":1,"semester":1,"name":"Maths","study_materials":[]}"""
        )
        assertEquals(0, dto.toDomain().materialCount)
    }

    @Test
    fun subjectDto_withMissingMaterialsField_givesZero() {
        val dto = json.decodeFromString<SubjectDto>(
            """{"id":"a","year":1,"semester":1,"name":"Maths"}"""
        )
        assertEquals(0, dto.toDomain().materialCount)
    }

    // --- StudyMaterialDto ---

    @Test
    fun studyMaterialDto_nullDescription_mapsToNull() {
        val dto = makeDto(description = null)
        assertNull(dto.toDomain().description)
    }

    @Test
    fun studyMaterialDto_blankDescription_mapsToNull() {
        val dto = makeDto(description = "   ")
        assertNull(dto.toDomain().description)
    }

    @Test
    fun studyMaterialDto_parsesCreatedAt() {
        val dto = makeDto(createdAt = "2026-10-14T10:00:00.123456+00:00")
        val domain = dto.toDomain()
        // should not throw
        assertEquals("2026-10-14T10:00:00.123456Z", domain.createdAt.toString())
    }

    // --- typeLabel ---

    @Test
    fun typeLabel_pdf() {
        val m = makeDto(fileName = "notes.pdf").toDomain()
        assertEquals("PDF", m.typeLabel)
    }

    @Test
    fun typeLabel_docxWithDots() {
        val m = makeDto(fileName = "a.b.docx").toDomain()
        assertEquals("DOCX", m.typeLabel)
    }

    @Test
    fun typeLabel_noExtension() {
        val m = makeDto(fileName = "noext").toDomain()
        assertEquals("", m.typeLabel)
    }

    // --- toFileSize ---

    @Test
    fun toFileSize_zero() {
        assertEquals("1 KB", 0L.toFileSize())
    }

    @Test
    fun toFileSize_1024() {
        assertEquals("1 KB", 1024L.toFileSize())
    }

    @Test
    fun toFileSize_2048() {
        assertEquals("2 KB", 2048L.toFileSize())
    }

    @Test
    fun toFileSize_1_5MB() {
        assertEquals("1.5 MB", 1572864L.toFileSize())
    }

    // --- Helper ---

    private fun makeDto(
        description: String? = null,
        fileName: String = "file.pdf",
        createdAt: String = "2026-10-14T10:00:00+00:00"
    ) = StudyMaterialDto(
        id = "id",
        subjectId = "sid",
        title = "title",
        description = description,
        filePath = "path",
        fileName = fileName,
        fileType = "application/pdf",
        fileSize = 1024,
        createdAt = createdAt
    )
}
