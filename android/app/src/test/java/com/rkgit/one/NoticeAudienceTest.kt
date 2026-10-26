package com.rkgit.one

import com.rkgit.one.data.profile.audienceCaption
import com.rkgit.one.data.profile.audienceText
import com.rkgit.one.data.profile.isAudienceFiltered
import com.rkgit.one.data.profile.matchesAudience
import com.rkgit.one.data.profile.matchingAudience
import com.rkgit.one.data.remote.dto.NoticeDto
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

class NoticeAudienceTest {

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

    private fun makeNotice(
        id: String,
        title: String,
        targetYears: List<Int> = emptyList(),
        targetDepartments: List<String> = emptyList()
    ) = Notice(
        id = id,
        title = title,
        body = null,
        category = NoticeCategory.GENERAL,
        pinned = false,
        linkUrl = null,
        attachment = null,
        publishedAt = Instant.parse("2026-10-20T10:00:00Z"),
        expiresAt = null,
        targetYears = targetYears,
        targetDepartments = targetDepartments
    )

    // --- matchesAudience full matrix ---

    @Test
    fun matchesAudience_everyoneNotice_matchesEveryFilter() {
        val years = emptyList<Int>()
        val depts = emptyList<String>()

        assertTrue(matchesAudience(years, depts, null, null))
        assertTrue(matchesAudience(years, depts, 1, null))
        assertTrue(matchesAudience(years, depts, null, "cse"))
        assertTrue(matchesAudience(years, depts, 2, "it"))
        assertTrue(matchesAudience(years, depts, 4, "me"))
    }

    @Test
    fun matchesAudience_year2Notice_matchesYear2AndNull_not1Or3() {
        val years = listOf(2)
        val depts = emptyList<String>()

        assertTrue(matchesAudience(years, depts, 2, null))
        assertTrue(matchesAudience(years, depts, null, null))
        assertFalse(matchesAudience(years, depts, 1, null))
        assertFalse(matchesAudience(years, depts, 3, null))
        assertFalse(matchesAudience(years, depts, 4, null))
    }

    @Test
    fun matchesAudience_cseNotice_matchesCseAndNull_notIt() {
        val years = emptyList<Int>()
        val depts = listOf("cse")

        assertTrue(matchesAudience(years, depts, null, "cse"))
        assertTrue(matchesAudience(years, depts, null, null))
        assertFalse(matchesAudience(years, depts, null, "it"))
        assertFalse(matchesAudience(years, depts, 2, "it"))
        assertTrue(matchesAudience(years, depts, 2, "cse"))
    }

    @Test
    fun matchesAudience_year2AndItNotice_needsBothToMatch() {
        val years = listOf(2)
        val depts = listOf("it")

        // year 2 with "cse" -> false
        assertFalse(matchesAudience(years, depts, 2, "cse"))
        // year 1 with "it" -> false
        assertFalse(matchesAudience(years, depts, 1, "it"))
        // year 2 with "it" -> true
        assertTrue(matchesAudience(years, depts, 2, "it"))
        // (null, "it") -> true
        assertTrue(matchesAudience(years, depts, null, "it"))
        // (2, null) -> true
        assertTrue(matchesAudience(years, depts, 2, null))
        // (null, null) -> true
        assertTrue(matchesAudience(years, depts, null, null))
    }

    @Test
    fun matchesAudience_severalYearsAndDepartments() {
        val years = listOf(1, 2)
        val depts = listOf("cse", "it")

        assertTrue(matchesAudience(years, depts, 1, "cse"))
        assertTrue(matchesAudience(years, depts, 2, "it"))
        assertTrue(matchesAudience(years, depts, 1, null))
        assertTrue(matchesAudience(years, depts, null, "cse"))
        assertTrue(matchesAudience(years, depts, null, null))

        assertFalse(matchesAudience(years, depts, 3, "cse"))
        assertFalse(matchesAudience(years, depts, 1, "me"))
        assertFalse(matchesAudience(years, depts, 4, "me"))
    }

    // --- matchingAudience keeps order and filters correctly ---

    @Test
    fun matchingAudience_fiveSampleNoticesMatrix() {
        val allNotice = makeNotice("1", "AudTest all", emptyList(), emptyList())
        val y1Notice = makeNotice("2", "AudTest Y1", listOf(1), emptyList())
        val y2ItNotice = makeNotice("3", "AudTest Y2 IT", listOf(2), listOf("it"))
        val cseNotice = makeNotice("4", "AudTest CSE", emptyList(), listOf("cse"))
        val y3MeNotice = makeNotice("5", "AudTest Y3 ME", listOf(3), listOf("me"))

        val noticeList = listOf(allNotice, y1Notice, y2ItNotice, cseNotice, y3MeNotice)

        // for filter (2,"it") the result is exactly [all, Y2 IT]
        val result2It = noticeList.matchingAudience(2, "it")
        assertEquals(listOf(allNotice, y2ItNotice), result2It)

        // for (1,"it") [all, Y1]
        val result1It = noticeList.matchingAudience(1, "it")
        assertEquals(listOf(allNotice, y1Notice), result1It)

        // for (null,"cse") [all, Y1, CSE]
        val resultNullCse = noticeList.matchingAudience(null, "cse")
        assertEquals(listOf(allNotice, y1Notice, cseNotice), resultNullCse)

        // for (null,null) all five
        val resultNullNull = noticeList.matchingAudience(null, null)
        assertEquals(noticeList, resultNullNull)

        // for (4,"me") [all]
        val result4Me = noticeList.matchingAudience(4, "me")
        assertEquals(listOf(allNotice), result4Me)
    }

    // --- audienceText ---

    @Test
    fun audienceText_allExamplesFromSpec() {
        // ([2],[it]) -> "Year 2 · IT"
        assertEquals("Year 2 · IT", audienceText(listOf(2), listOf("it")))

        // ([1,2],[cse,it]) -> "Year 1, 2 · CSE, IT"
        assertEquals("Year 1, 2 · CSE, IT", audienceText(listOf(1, 2), listOf("cse", "it")))

        // ([],[cse]) -> "All years · CSE"
        assertEquals("All years · CSE", audienceText(emptyList(), listOf("cse")))

        // ([1],[]) -> "Year 1 · All branches"
        assertEquals("Year 1 · All branches", audienceText(listOf(1), emptyList()))

        // ([],[]) -> null
        assertNull(audienceText(emptyList(), emptyList()))

        // unsorted years [3,1,3] -> "Year 1, 3"
        assertEquals("Year 1, 3 · All branches", audienceText(listOf(3, 1, 3), emptyList()))

        // duplicate departments [it,it] -> "IT"
        assertEquals("All years · IT", audienceText(emptyList(), listOf("it", "it")))
    }

    // --- audienceCaption ---

    @Test
    fun audienceCaption_examples() {
        // (null,null) -> "Showing all notices"
        assertEquals("Showing all notices", audienceCaption(null, null))

        // (2,"it") -> "Showing notices for Year 2 · IT"
        assertEquals("Showing notices for Year 2 · IT", audienceCaption(2, "it"))

        // (null,"it") -> "Showing notices for all years · IT"
        assertEquals("Showing notices for all years · IT", audienceCaption(null, "it"))

        // (3,null) -> "Showing notices for Year 3 · all branches"
        assertEquals("Showing notices for Year 3 · all branches", audienceCaption(3, null))
    }

    // --- isAudienceFiltered ---

    @Test
    fun isAudienceFiltered_testCases() {
        assertFalse(isAudienceFiltered(null, null))
        assertTrue(isAudienceFiltered(1, null))
        assertTrue(isAudienceFiltered(null, "cse"))
        assertTrue(isAudienceFiltered(2, "it"))
    }

    // --- NoticeDto decoding ---

    @Test
    fun noticeDto_decoding_targetYearsAndDepartments() {
        // arrays present -> mapped
        val jsonFull = """
            {
                "id": "n1",
                "title": "Title 1",
                "category": "general",
                "pinned": false,
                "published_at": "2026-10-20T10:00:00+00:00",
                "target_years": [1, 2],
                "target_departments": ["cse", "it"]
            }
        """.trimIndent()
        val dtoFull = json.decodeFromString<NoticeDto>(jsonFull).toDomain()
        assertEquals(listOf(1, 2), dtoFull.targetYears)
        assertEquals(listOf("cse", "it"), dtoFull.targetDepartments)

        // fields missing -> empty lists
        val jsonMissing = """
            {
                "id": "n2",
                "title": "Title 2",
                "category": "general",
                "pinned": false,
                "published_at": "2026-10-20T10:00:00+00:00"
            }
        """.trimIndent()
        val dtoMissing = json.decodeFromString<NoticeDto>(jsonMissing).toDomain()
        assertEquals(emptyList<Int>(), dtoMissing.targetYears)
        assertEquals(emptyList<String>(), dtoMissing.targetDepartments)

        // null arrays -> empty
        val jsonNulls = """
            {
                "id": "n3",
                "title": "Title 3",
                "category": "general",
                "pinned": false,
                "published_at": "2026-10-20T10:00:00+00:00",
                "target_years": null,
                "target_departments": null
            }
        """.trimIndent()
        val dtoNulls = json.decodeFromString<NoticeDto>(jsonNulls).toDomain()
        assertEquals(emptyList<Int>(), dtoNulls.targetYears)
        assertEquals(emptyList<String>(), dtoNulls.targetDepartments)

        // empty arrays -> empty
        val jsonEmpty = """
            {
                "id": "n4",
                "title": "Title 4",
                "category": "general",
                "pinned": false,
                "published_at": "2026-10-20T10:00:00+00:00",
                "target_years": [],
                "target_departments": []
            }
        """.trimIndent()
        val dtoEmpty = json.decodeFromString<NoticeDto>(jsonEmpty).toDomain()
        assertEquals(emptyList<Int>(), dtoEmpty.targetYears)
        assertEquals(emptyList<String>(), dtoEmpty.targetDepartments)

        // [1,null,2] -> [1,2]
        val jsonNullElement = """
            {
                "id": "n5",
                "title": "Title 5",
                "category": "general",
                "pinned": false,
                "published_at": "2026-10-20T10:00:00+00:00",
                "target_years": [1, null, 2]
            }
        """.trimIndent()
        val dtoNullElement = json.decodeFromString<NoticeDto>(jsonNullElement).toDomain()
        assertEquals(listOf(1, 2), dtoNullElement.targetYears)

        // [0,5,2] -> [2]
        val jsonOutOfRange = """
            {
                "id": "n6",
                "title": "Title 6",
                "category": "general",
                "pinned": false,
                "published_at": "2026-10-20T10:00:00+00:00",
                "target_years": [0, 5, 2]
            }
        """.trimIndent()
        val dtoOutOfRange = json.decodeFromString<NoticeDto>(jsonOutOfRange).toDomain()
        assertEquals(listOf(2), dtoOutOfRange.targetYears)

        // ["cse",null] -> ["cse"]
        val jsonNullDept = """
            {
                "id": "n7",
                "title": "Title 7",
                "category": "general",
                "pinned": false,
                "published_at": "2026-10-20T10:00:00+00:00",
                "target_departments": ["cse", null]
            }
        """.trimIndent()
        val dtoNullDept = json.decodeFromString<NoticeDto>(jsonNullDept).toDomain()
        assertEquals(listOf("cse"), dtoNullDept.targetDepartments)

        // duplicates removed
        val jsonDuplicates = """
            {
                "id": "n8",
                "title": "Title 8",
                "category": "general",
                "pinned": false,
                "published_at": "2026-10-20T10:00:00+00:00",
                "target_years": [2, 2, 3],
                "target_departments": ["cse", "cse", "it"]
            }
        """.trimIndent()
        val dtoDuplicates = json.decodeFromString<NoticeDto>(jsonDuplicates).toDomain()
        assertEquals(listOf(2, 3), dtoDuplicates.targetYears)
        assertEquals(listOf("cse", "it"), dtoDuplicates.targetDepartments)
    }

    // --- HomeData ---

    @Test
    fun homeData_visibleNotices_appliesProfileAndTakesAtMost3InOrder() {
        val n1 = makeNotice("1", "N1", listOf(2), listOf("it"))
        val n2 = makeNotice("2", "N2", emptyList(), emptyList())
        val n3 = makeNotice("3", "N3", listOf(1), emptyList())
        val n4 = makeNotice("4", "N4", listOf(2), listOf("it"))
        val n5 = makeNotice("5", "N5", emptyList(), listOf("it"))

        val data = HomeData(
            notices = listOf(n1, n2, n3, n4, n5),
            materials = emptyList(),
            events = emptyList()
        )

        // With Year 2 + IT: matching are n1, n2, n4, n5. take(3) gives n1, n2, n4
        val visible = data.visibleNotices(2, "it")
        assertEquals(3, visible.size)
        assertEquals(listOf(n1, n2, n4), visible)
    }

    @Test
    fun homeData_isEmptyFor_andExistingIsEmpty() {
        val nY1 = makeNotice("1", "N1", listOf(1), emptyList())
        val sampleMaterial = RecentMaterial(
            material = StudyMaterial(
                id = "m1",
                subjectId = "s1",
                title = "Mat",
                description = null,
                fileName = "f.pdf",
                filePath = "p",
                fileType = "application/pdf",
                fileSize = 100L,
                createdAt = Instant.now()
            ),
            subjectName = "Physics",
            year = 1,
            semester = 1
        )
        val sampleEvent = Event(
            id = "e1",
            title = "Evt",
            description = null,
            eventDate = Instant.now(),
            venue = null,
            posterUrl = null,
            registrationUrl = null
        )

        val dataOnlyY1 = HomeData(
            notices = listOf(nY1),
            materials = emptyList(),
            events = emptyList()
        )

        // Existing isEmpty is false because notices is not empty
        assertFalse(dataOnlyY1.isEmpty)

        // But for Year 2, visible notices is empty and materials & events are empty -> isEmptyFor is true
        assertTrue(dataOnlyY1.isEmptyFor(2, "it"))

        // For Year 1 -> visible notices has nY1 -> isEmptyFor is false
        assertFalse(dataOnlyY1.isEmptyFor(1, null))

        // If materials present, isEmptyFor is false even if notices filtered out
        val dataWithMaterial = HomeData(
            notices = listOf(nY1),
            materials = listOf(sampleMaterial),
            events = emptyList()
        )
        assertFalse(dataWithMaterial.isEmptyFor(2, "it"))

        // If events present, isEmptyFor is false even if notices filtered out
        val dataWithEvent = HomeData(
            notices = listOf(nY1),
            materials = emptyList(),
            events = listOf(sampleEvent)
        )
        assertFalse(dataWithEvent.isEmptyFor(2, "it"))

        // When all truly empty
        val dataAllEmpty = HomeData(
            notices = emptyList(),
            materials = emptyList(),
            events = emptyList()
        )
        assertTrue(dataAllEmpty.isEmpty)
        assertTrue(dataAllEmpty.isEmptyFor(null, null))
        assertTrue(dataAllEmpty.isEmptyFor(2, "it"))
    }
}
