package com.rkgit.one

import com.rkgit.one.data.local.DownloadedFile
import com.rkgit.one.data.remote.dto.SearchRowDto
import com.rkgit.one.data.remote.dto.toSearchResults
import com.rkgit.one.data.search.isSearchable
import com.rkgit.one.data.search.normalizeQuery
import com.rkgit.one.data.search.searchDownloads
import com.rkgit.one.domain.model.NoticeCategory
import com.rkgit.one.domain.model.SearchEvent
import com.rkgit.one.domain.model.SearchMaterial
import com.rkgit.one.domain.model.SearchNotice
import com.rkgit.one.domain.model.SearchResults
import com.rkgit.one.ui.navigation.toViewerRoute
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

class SearchLogicTest {

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
    fun testNormalizeQuery() {
        assertEquals("", normalizeQuery(null))
        assertEquals("ds unit", normalizeQuery("  ds   unit  "))
        assertEquals("ds unit", normalizeQuery("\tds\n\nunit\t"))
        val longA = "a".repeat(150)
        val normalizedLongA = normalizeQuery(longA)
        assertEquals(100, normalizedLongA.length)
        assertEquals("a".repeat(100), normalizedLongA)

        // a 101-character string with a space at index 99 does not end with a space
        val string101 = "a".repeat(99) + " " + "b"
        assertEquals(101, string101.length)
        assertEquals(' ', string101[99])
        val normalized101 = normalizeQuery(string101)
        assertFalse(normalized101.endsWith(" "))
        assertEquals(99, normalized101.length)
        assertEquals("a".repeat(99), normalized101)
    }

    @Test
    fun testIsSearchable() {
        assertFalse(isSearchable(""))
        assertFalse(isSearchable("a"))
        assertFalse(isSearchable(normalizeQuery(" a ")))
        assertTrue(isSearchable("ab"))
    }

    @Test
    fun testToSearchResults() {
        val jsonString = """
            [
              {
                "kind": "material",
                "id": "11111111-1111-1111-1111-111111111111",
                "title": "Data Structures Unit 1",
                "subtitle": "Data Structures",
                "snippet": "First material snippet",
                "rank": 1,
                "sort_date": "2026-10-15T10:00:00Z",
                "extra": {
                  "subject_id": "sub-1",
                  "subject_name": "Data Structures",
                  "year": 2,
                  "semester": 3,
                  "file_path": "materials/ds1.pdf",
                  "file_name": "ds1.pdf",
                  "file_type": "application/pdf",
                  "file_size": 2402417
                }
              },
              {
                "kind": "material",
                "id": "11111111-1111-1111-1111-111111111112",
                "title": "Data Structures Unit 2",
                "subtitle": "Data Structures",
                "snippet": "   ",
                "rank": 2,
                "sort_date": "2026-10-16T10:00:00Z",
                "extra": {
                  "subject_id": "sub-1",
                  "subject_name": "Data Structures",
                  "year": 2,
                  "semester": 3,
                  "file_path": "materials/ds2.pdf",
                  "file_name": "ds2.pdf",
                  "file_type": "application/pdf",
                  "file_size": 15000000000
                }
              },
              {
                "kind": "event",
                "id": "22222222-2222-2222-2222-222222222222",
                "title": "Hackathon 2026",
                "subtitle": null,
                "snippet": "Hackathon snippet",
                "rank": 1,
                "sort_date": "2026-10-20T14:30:00.123456+05:30",
                "extra": {
                  "event_date": "2026-10-20T14:30:00.123456+05:30",
                  "venue": null
                }
              },
              {
                "kind": "notice",
                "id": "33333333-3333-3333-3333-333333333333",
                "title": "Odd Semester Registration",
                "subtitle": "Registration",
                "snippet": "Notice snippet",
                "rank": 1,
                "sort_date": "2026-10-18T09:00:00Z",
                "extra": {
                  "category": "weird",
                  "pinned": true,
                  "has_attachment": false
                }
              },
              {
                "kind": "foo",
                "id": "44444444-4444-4444-4444-444444444444",
                "title": "Unknown Kind",
                "extra": {}
              },
              {
                "kind": "material",
                "id": "55555555-5555-5555-5555-555555555555",
                "title": "Malformed Extra Material",
                "extra": {
                  "file_size": "not-a-number-and-missing-fields"
                }
              }
            ]
        """.trimIndent()

        val rows = json.decodeFromString<List<SearchRowDto>>(jsonString)
        val results = rows.toSearchResults(json)

        // Asserts counts (2 materials to test order preservation, 1 event, 1 notice; unknown and malformed ignored)
        assertEquals(2, results.materials.size)
        assertEquals(1, results.events.size)
        assertEquals(1, results.notices.size)

        // Preserves order inside kind
        assertEquals("Data Structures Unit 1", results.materials[0].title)
        assertEquals("Data Structures Unit 2", results.materials[1].title)

        // Blank snippet becomes null
        assertNull(results.materials[1].snippet)
        assertEquals("First material snippet", results.materials[0].snippet)

        // file_size as a JSON number maps to Long
        assertEquals(2402417L, results.materials[0].fileSize)
        assertEquals(15000000000L, results.materials[1].fileSize)

        // Event venue null, timestamp with microseconds parses to Instant
        val event = results.events[0]
        assertEquals("Hackathon 2026", event.title)
        assertNull(event.venue)
        assertEquals(Instant.parse("2026-10-20T09:00:00.123456Z"), event.eventDate)

        // Unknown category becomes GENERAL, pinned true, hasAttachment false
        val notice = results.notices[0]
        assertEquals(NoticeCategory.GENERAL, notice.category)
        assertTrue(notice.pinned)
        assertFalse(notice.hasAttachment)
    }

    @Test
    fun testSearchDownloads() {
        val row1 = DownloadedFile(
            kind = "material",
            itemId = "1",
            title = "Data Structures Notes",
            subtitle = "Computer Science",
            year = 2,
            semester = 3,
            fileName = "ds_notes.pdf",
            fileType = "application/pdf",
            sizeBytes = 1000L,
            remoteFilePath = "path1",
            localPath = "/local/path1",
            downloadedAt = 1000L
        )
        val row2 = DownloadedFile(
            kind = "material",
            itemId = "2",
            title = "Algorithms Analysis",
            subtitle = "Data Structures Dept",
            year = 2,
            semester = 4,
            fileName = "algo.pdf",
            fileType = "application/pdf",
            sizeBytes = 2000L,
            remoteFilePath = "path2",
            localPath = "/local/path2",
            downloadedAt = 3000L
        )
        val row3 = DownloadedFile(
            kind = "notice",
            itemId = "3",
            title = "Holiday Notice",
            subtitle = "General",
            year = null,
            semester = null,
            fileName = "diwali_circular.pdf",
            fileType = "application/pdf",
            sizeBytes = 500L,
            remoteFilePath = "path3",
            localPath = "/local/path3",
            downloadedAt = 2000L
        )
        val rowSpecial = DownloadedFile(
            kind = "material",
            itemId = "4",
            title = "Special 100%_score.final",
            subtitle = "Bonus",
            year = 1,
            semester = 1,
            fileName = "exam_100%.pdf",
            fileType = "application/pdf",
            sizeBytes = 300L,
            remoteFilePath = "path4",
            localPath = "/local/path4",
            downloadedAt = 4000L
        )

        val allRows = listOf(row1, row2, row3, rowSpecial)

        // matches by title case-insensitively
        val titleMatches = searchDownloads(allRows, "structures")
        assertEquals(2, titleMatches.size)
        // sorted by downloadedAt descending (newest first: row2 is 3000L, row1 is 1000L)
        assertEquals("Algorithms Analysis", titleMatches[0].title)
        assertEquals("Data Structures Notes", titleMatches[1].title)

        // matches by subtitle case-insensitively
        val subtitleMatches = searchDownloads(allRows, "science")
        assertEquals(1, subtitleMatches.size)
        assertEquals("Data Structures Notes", subtitleMatches[0].title)

        // matches by fileName case-insensitively
        val fileNameMatches = searchDownloads(allRows, "diwali")
        assertEquals(1, fileNameMatches.size)
        assertEquals("Holiday Notice", fileNameMatches[0].title)

        // "%", "_", "." match only literal occurrences, not as wildcards
        val pctMatches = searchDownloads(allRows, "%")
        assertEquals(0, pctMatches.size) // length 1 is not searchable!
        val pctMatches2 = searchDownloads(allRows, "0%")
        assertEquals(1, pctMatches2.size)
        assertEquals("Special 100%_score.final", pctMatches2[0].title)

        val underscoreMatches = searchDownloads(allRows, "%_")
        assertEquals(1, underscoreMatches.size)
        assertEquals("Special 100%_score.final", underscoreMatches[0].title)

        val dotMatches = searchDownloads(allRows, ".final")
        assertEquals(1, dotMatches.size)
        assertEquals("Special 100%_score.final", dotMatches[0].title)

        // short query -> empty
        assertEquals(0, searchDownloads(allRows, "a").size)
        assertEquals(0, searchDownloads(allRows, " ").size)

        // empty input -> empty
        assertEquals(0, searchDownloads(emptyList(), "structures").size)
    }

    @Test
    fun testSearchMaterialToViewerRoute() {
        val material = SearchMaterial(
            id = "mat-123",
            title = "Unit 1 Lecture Notes",
            subjectId = "sub-456",
            subjectName = "Database Systems",
            year = 3,
            semester = 5,
            snippet = "SQL and Normalization",
            filePath = "materials/db_unit1.pdf",
            fileName = "db_unit1.pdf",
            fileType = "application/pdf",
            fileSize = 1048576L
        )

        assertEquals("PDF", material.typeLabel)

        val route = material.toViewerRoute()
        assertEquals("material", route.kind)
        assertEquals("mat-123", route.itemId)
        assertEquals("Unit 1 Lecture Notes", route.title)
        assertEquals("db_unit1.pdf", route.fileName)
        assertEquals("application/pdf", route.fileType)
        assertEquals(1048576L, route.fileSize)
        assertEquals("materials/db_unit1.pdf", route.remotePath)
        assertEquals("Database Systems", route.subtitle)
        assertEquals(3, route.year)
        assertEquals(5, route.semester)
    }

    @Test
    fun testSearchResultsTotalAndIsEmpty() {
        val empty = SearchResults(emptyList(), emptyList(), emptyList())
        assertEquals(0, empty.total)
        assertTrue(empty.isEmpty)

        val material = SearchMaterial(
            id = "m1",
            title = "Mat 1",
            subjectId = "s1",
            subjectName = "Sub 1",
            year = 1,
            semester = 1,
            snippet = null,
            filePath = "p1",
            fileName = "f1.pdf",
            fileType = "application/pdf",
            fileSize = 100L
        )
        val notEmpty = SearchResults(listOf(material), emptyList(), emptyList())
        assertEquals(1, notEmpty.total)
        assertFalse(notEmpty.isEmpty)
    }
}
