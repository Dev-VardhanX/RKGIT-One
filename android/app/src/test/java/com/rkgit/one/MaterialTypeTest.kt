package com.rkgit.one

import com.rkgit.one.data.remote.dto.RecentMaterialDto
import com.rkgit.one.data.remote.dto.SearchRowDto
import com.rkgit.one.data.remote.dto.StudyMaterialDto
import com.rkgit.one.data.remote.dto.toDomain
import com.rkgit.one.data.remote.dto.toSearchResults
import com.rkgit.one.domain.model.MaterialType
import com.rkgit.one.domain.model.StudyMaterial
import com.rkgit.one.domain.model.availableTypes
import com.rkgit.one.domain.model.filterByType
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.util.Locale

class MaterialTypeTest {

    private lateinit var originalLocale: Locale
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setUp() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun testFromValue() {
        assertEquals(MaterialType.NOTES, MaterialType.fromValue("notes"))
        assertEquals(MaterialType.SYLLABUS, MaterialType.fromValue("syllabus"))
        assertEquals(MaterialType.ST_EXAM, MaterialType.fromValue("st_exam"))
        assertEquals(MaterialType.PUT_EXAM, MaterialType.fromValue("put_exam"))
        assertEquals(MaterialType.EXTERNAL_EXAM, MaterialType.fromValue("external_exam"))
        assertEquals(MaterialType.LAB_MANUAL, MaterialType.fromValue("lab_manual"))

        assertEquals(MaterialType.NOTES, MaterialType.fromValue("unknown"))
        assertEquals(MaterialType.NOTES, MaterialType.fromValue(""))
        assertEquals(MaterialType.NOTES, MaterialType.fromValue(null))
    }

    @Test
    fun testEnumOrderAndLabels() {
        val expectedOrder = listOf(
            MaterialType.NOTES,
            MaterialType.SYLLABUS,
            MaterialType.ST_EXAM,
            MaterialType.PUT_EXAM,
            MaterialType.EXTERNAL_EXAM,
            MaterialType.LAB_MANUAL
        )
        assertEquals(expectedOrder, MaterialType.entries)

        assertEquals("Notes", MaterialType.NOTES.label)
        assertEquals("Syllabus", MaterialType.SYLLABUS.label)
        assertEquals("ST Exam", MaterialType.ST_EXAM.label)
        assertEquals("PUT Exam", MaterialType.PUT_EXAM.label)
        assertEquals("External Exam", MaterialType.EXTERNAL_EXAM.label)
        assertEquals("Lab Manual", MaterialType.LAB_MANUAL.label)
    }

    @Test
    fun testAvailableTypes() {
        assertTrue(availableTypes(emptyList()).isEmpty())

        val single = listOf(createMaterial(id = "1", type = MaterialType.NOTES))
        assertEquals(listOf(MaterialType.NOTES), availableTypes(single))

        // input in mixed order with duplicates returns distinct types in enum order
        val mixed = listOf(
            createMaterial(id = "1", type = MaterialType.LAB_MANUAL),
            createMaterial(id = "2", type = MaterialType.SYLLABUS),
            createMaterial(id = "3", type = MaterialType.LAB_MANUAL),
            createMaterial(id = "4", type = MaterialType.NOTES),
            createMaterial(id = "5", type = MaterialType.EXTERNAL_EXAM),
            createMaterial(id = "6", type = MaterialType.SYLLABUS)
        )
        val result = availableTypes(mixed)
        assertEquals(
            listOf(
                MaterialType.NOTES,
                MaterialType.SYLLABUS,
                MaterialType.EXTERNAL_EXAM,
                MaterialType.LAB_MANUAL
            ),
            result
        )
    }

    @Test
    fun testFilterByType() {
        val m1 = createMaterial(id = "1", type = MaterialType.NOTES)
        val m2 = createMaterial(id = "2", type = MaterialType.SYLLABUS)
        val m3 = createMaterial(id = "3", type = MaterialType.NOTES)
        val list = listOf(m1, m2, m3)

        // null returns everything unchanged in the same order
        assertEquals(list, filterByType(list, null))

        // a type returns only that type and keeps the original relative order
        val notesOnly = filterByType(list, MaterialType.NOTES)
        assertEquals(listOf(m1, m3), notesOnly)

        val syllabusOnly = filterByType(list, MaterialType.SYLLABUS)
        assertEquals(listOf(m2), syllabusOnly)

        // a type with no files returns empty
        val labOnly = filterByType(list, MaterialType.LAB_MANUAL)
        assertTrue(labOnly.isEmpty())
    }

    @Test
    fun testStudyMaterialDtoDecoding() {
        // with material_type = "syllabus" -> SYLLABUS
        val jsonSyllabus = """
            {
                "id": "1",
                "subject_id": "sub-1",
                "title": "Syllabus File",
                "file_path": "path/1.pdf",
                "file_name": "1.pdf",
                "file_type": "application/pdf",
                "file_size": 1024,
                "created_at": "2026-10-20T10:00:00Z",
                "material_type": "syllabus"
            }
        """.trimIndent()
        val dtoSyllabus = json.decodeFromString<StudyMaterialDto>(jsonSyllabus)
        assertEquals(MaterialType.SYLLABUS, dtoSyllabus.toDomain().type)

        // with field MISSING -> NOTES
        val jsonMissing = """
            {
                "id": "2",
                "subject_id": "sub-1",
                "title": "Missing Type File",
                "file_path": "path/2.pdf",
                "file_name": "2.pdf",
                "file_type": "application/pdf",
                "file_size": 1024,
                "created_at": "2026-10-20T10:00:00Z"
            }
        """.trimIndent()
        val dtoMissing = json.decodeFromString<StudyMaterialDto>(jsonMissing)
        assertEquals(MaterialType.NOTES, dtoMissing.toDomain().type)

        // with unknown value -> NOTES
        val jsonUnknown = """
            {
                "id": "3",
                "subject_id": "sub-1",
                "title": "Unknown Type File",
                "file_path": "path/3.pdf",
                "file_name": "3.pdf",
                "file_type": "application/pdf",
                "file_size": 1024,
                "created_at": "2026-10-20T10:00:00Z",
                "material_type": "unknown_value"
            }
        """.trimIndent()
        val dtoUnknown = json.decodeFromString<StudyMaterialDto>(jsonUnknown)
        assertEquals(MaterialType.NOTES, dtoUnknown.toDomain().type)
    }

    @Test
    fun testRecentMaterialDtoDecoding() {
        val jsonRecent = """
            {
                "id": "rec-1",
                "subject_id": "sub-1",
                "title": "ST Exam Paper",
                "file_path": "path/st.pdf",
                "file_name": "st.pdf",
                "file_type": "application/pdf",
                "file_size": 2048,
                "created_at": "2026-10-20T10:00:00Z",
                "material_type": "st_exam",
                "subjects": {
                    "name": "Data Structures",
                    "year": 2,
                    "semester": 3
                }
            }
        """.trimIndent()
        val dtoRecent = json.decodeFromString<RecentMaterialDto>(jsonRecent)
        val domain = dtoRecent.toDomain()
        assertEquals(MaterialType.ST_EXAM, domain.material.type)
    }

    @Test
    fun testSearchMaterialExtraDecoding() {
        // material row whose extra contains "material_type":"lab_manual" maps to LAB_MANUAL
        val jsonLab = """
            [
                {
                    "kind": "material",
                    "id": "m-lab",
                    "title": "OS Lab Manual",
                    "extra": {
                        "subject_id": "sub-os",
                        "subject_name": "Operating Systems",
                        "material_type": "lab_manual"
                    }
                }
            ]
        """.trimIndent()
        val rowsLab = json.decodeFromString<List<SearchRowDto>>(jsonLab)
        val resultsLab = rowsLab.toSearchResults(json)
        assertEquals(1, resultsLab.materials.size)
        assertEquals(MaterialType.LAB_MANUAL, resultsLab.materials[0].type)

        // extra without the field maps to NOTES
        val jsonMissing = """
            [
                {
                    "kind": "material",
                    "id": "m-missing",
                    "title": "Notes Material",
                    "extra": {
                        "subject_id": "sub-os",
                        "subject_name": "Operating Systems"
                    }
                }
            ]
        """.trimIndent()
        val rowsMissing = json.decodeFromString<List<SearchRowDto>>(jsonMissing)
        val resultsMissing = rowsMissing.toSearchResults(json)
        assertEquals(1, resultsMissing.materials.size)
        assertEquals(MaterialType.NOTES, resultsMissing.materials[0].type)

        // extra with "material_type": null maps to NOTES
        val jsonNull = """
            [
                {
                    "kind": "material",
                    "id": "m-null",
                    "title": "Null Type Material",
                    "extra": {
                        "subject_id": "sub-os",
                        "subject_name": "Operating Systems",
                        "material_type": null
                    }
                }
            ]
        """.trimIndent()
        val rowsNull = json.decodeFromString<List<SearchRowDto>>(jsonNull)
        val resultsNull = rowsNull.toSearchResults(json)
        assertEquals(1, resultsNull.materials.size)
        assertEquals(MaterialType.NOTES, resultsNull.materials[0].type)
    }

    private fun createMaterial(id: String, type: MaterialType): StudyMaterial =
        StudyMaterial(
            id = id,
            subjectId = "sub-1",
            title = "Title $id",
            description = null,
            fileName = "file$id.pdf",
            filePath = "path/$id.pdf",
            fileType = "application/pdf",
            fileSize = 1024L,
            createdAt = Instant.now(),
            type = type
        )
}
