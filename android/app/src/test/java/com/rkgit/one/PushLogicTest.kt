package com.rkgit.one

import com.rkgit.one.push.DeepLink
import com.rkgit.one.push.EXTRA_PUSH_ID
import com.rkgit.one.push.EXTRA_PUSH_TYPE
import com.rkgit.one.push.PushMessage
import com.rkgit.one.push.PushType
import com.rkgit.one.push.notificationId
import com.rkgit.one.push.notificationText
import com.rkgit.one.push.parseDeepLink
import com.rkgit.one.push.parsePush
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.Locale

class PushLogicTest {

    private lateinit var originalLocale: Locale

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
    fun parsePush_validNoticeAndEvent() {
        val noticeData = mapOf(
            "type" to "notice",
            "id" to "123e4567-e89b-12d3-a456-426614174000",
            "title" to "Notice Title",
            "category" to "datesheet"
        )
        val notice = parsePush(noticeData)
        assertNotNull(notice)
        assertEquals(PushType.NOTICE, notice?.type)
        assertEquals("123e4567-e89b-12d3-a456-426614174000", notice?.id)
        assertEquals("Notice Title", notice?.title)
        assertEquals("datesheet", notice?.category)
        assertNull(notice?.eventDate)
        assertNull(notice?.venue)

        val eventData = mapOf(
            "type" to "event",
            "id" to "223e4567-e89b-12d3-a456-426614174000",
            "title" to "Event Title",
            "event_date" to "2026-11-05T10:00:00+00:00",
            "venue" to "Auditorium"
        )
        val event = parsePush(eventData)
        assertNotNull(event)
        assertEquals(PushType.EVENT, event?.type)
        assertEquals("223e4567-e89b-12d3-a456-426614174000", event?.id)
        assertEquals("Event Title", event?.title)
        assertEquals("2026-11-05T10:00:00+00:00", event?.eventDate)
        assertEquals("Auditorium", event?.venue)
    }

    @Test
    fun parsePush_missingTypeOrId() {
        val noType = mapOf("id" to "123e4567-e89b-12d3-a456-426614174000", "title" to "Title")
        assertNull(parsePush(noType))

        val noId = mapOf("type" to "notice", "title" to "Title")
        assertNull(parsePush(noId))
    }

    @Test
    fun parsePush_unknownTypeAndUpperCase() {
        val unknown = mapOf("type" to "other", "id" to "123e4567-e89b-12d3-a456-426614174000", "title" to "Title")
        assertNull(parsePush(unknown))

        val upperCase = mapOf("type" to "NOTICE", "id" to "123e4567-e89b-12d3-a456-426614174000", "title" to "Title")
        assertNull(parsePush(upperCase))
    }

    @Test
    fun parsePush_invalidIdNotUuid() {
        val invalidId = mapOf("type" to "notice", "id" to "123", "title" to "Title")
        assertNull(parsePush(invalidId))

        val badFormat = mapOf("type" to "notice", "id" to "123e4567-e89b-12d3-a456-42661417400Z", "title" to "Title")
        assertNull(parsePush(badFormat))
    }

    @Test
    fun parsePush_blankTitle() {
        val emptyTitle = mapOf("type" to "notice", "id" to "123e4567-e89b-12d3-a456-426614174000", "title" to "")
        assertNull(parsePush(emptyTitle))

        val blankTitle = mapOf("type" to "notice", "id" to "123e4567-e89b-12d3-a456-426614174000", "title" to "   ")
        assertNull(parsePush(blankTitle))
    }

    @Test
    fun parsePush_longTitleCutTo200CodePoints() {
        val longTitle = "A".repeat(300)
        val data = mapOf(
            "type" to "notice",
            "id" to "123e4567-e89b-12d3-a456-426614174000",
            "title" to longTitle
        )
        val msg = parsePush(data)
        assertNotNull(msg)
        assertEquals(200, msg?.title?.length)
        assertEquals("A".repeat(200), msg?.title)
    }

    @Test
    fun parsePush_blankCategoryVenueEventDateBecomeNull() {
        val data = mapOf(
            "type" to "event",
            "id" to "123e4567-e89b-12d3-a456-426614174000",
            "title" to "Event Title",
            "category" to "   ",
            "event_date" to "",
            "venue" to "  "
        )
        val msg = parsePush(data)
        assertNotNull(msg)
        assertNull(msg?.category)
        assertNull(msg?.eventDate)
        assertNull(msg?.venue)
    }

    @Test
    fun parsePush_extraKeysIgnored() {
        val data = mapOf(
            "type" to "notice",
            "id" to "123e4567-e89b-12d3-a456-426614174000",
            "title" to "Title",
            "extra_key" to "value",
            "random" to "123"
        )
        val msg = parsePush(data)
        assertNotNull(msg)
        assertEquals("123e4567-e89b-12d3-a456-426614174000", msg?.id)
    }

    @Test
    fun parsePush_upperCaseUuidAccepted() {
        val data = mapOf(
            "type" to "notice",
            "id" to "123E4567-E89B-12D3-A456-426614174000",
            "title" to "Title"
        )
        val msg = parsePush(data)
        assertNotNull(msg)
        assertEquals("123E4567-E89B-12D3-A456-426614174000", msg?.id)
    }

    @Test
    fun notificationText_noticeCategories() {
        val msgDatesheet = PushMessage(PushType.NOTICE, "id1", "Datesheet Out", "datesheet", null, null)
        val textDatesheet = notificationText(msgDatesheet) { "FORMATTED" }
        assertEquals("New notice · Datesheet", textDatesheet.title)
        assertEquals("Datesheet Out", textDatesheet.body)

        val msgHoliday = PushMessage(PushType.NOTICE, "id2", "Diwali Break", "holiday", null, null)
        val textHoliday = notificationText(msgHoliday) { "FORMATTED" }
        assertEquals("New notice · Holiday", textHoliday.title)
        assertEquals("Diwali Break", textHoliday.body)

        val msgUnknown = PushMessage(PushType.NOTICE, "id3", "Special Announcement", "unknown_category", null, null)
        val textUnknown = notificationText(msgUnknown) { "FORMATTED" }
        assertEquals("New notice · General", textUnknown.title)
        assertEquals("Special Announcement", textUnknown.body)

        val msgNullCat = PushMessage(PushType.NOTICE, "id4", "No Category", null, null, null)
        val textNullCat = notificationText(msgNullCat) { "FORMATTED" }
        assertEquals("New notice", textNullCat.title)
        assertEquals("No Category", textNullCat.body)

        val msgBlankCat = PushMessage(PushType.NOTICE, "id5", "Blank Category", "   ", null, null)
        val textBlankCat = notificationText(msgBlankCat) { "FORMATTED" }
        assertEquals("New notice", textBlankCat.title)
        assertEquals("Blank Category", textBlankCat.body)
    }

    @Test
    fun notificationText_eventWithValidAndInvalidDate() {
        var formatCalled = false
        val msgValidDate = PushMessage(PushType.EVENT, "id1", "Annual Fest", null, "2026-11-05T10:00:00+00:00", null)
        val textValid = notificationText(msgValidDate) {
            formatCalled = true
            "DATE"
        }
        assertEquals("New event", textValid.title)
        assertEquals("Annual Fest · DATE", textValid.body)
        assertEquals(true, formatCalled)

        var formatCalledInvalid = false
        val msgInvalidDate = PushMessage(PushType.EVENT, "id2", "Hackathon", null, "invalid-date", null)
        val textInvalid = notificationText(msgInvalidDate) {
            formatCalledInvalid = true
            "DATE"
        }
        assertEquals("New event", textInvalid.title)
        assertEquals("Hackathon", textInvalid.body)
        assertEquals(false, formatCalledInvalid)

        val msgNullDate = PushMessage(PushType.EVENT, "id3", "Workshop", null, null, null)
        val textNullDate = notificationText(msgNullDate) { "DATE" }
        assertEquals("New event", textNullDate.title)
        assertEquals("Workshop", textNullDate.body)
    }

    @Test
    fun notificationId_stabilityAndDifferentiation() {
        val notice1 = PushMessage(PushType.NOTICE, "123e4567-e89b-12d3-a456-426614174000", "Title", null, null, null)
        val notice2 = PushMessage(PushType.NOTICE, "123e4567-e89b-12d3-a456-426614174000", "Other Title", "datesheet", null, null)
        assertEquals(notificationId(notice1), notificationId(notice2))

        val event = PushMessage(PushType.EVENT, "123e4567-e89b-12d3-a456-426614174000", "Title", null, null, null)
        assertNotEquals(notificationId(notice1), notificationId(event))
    }

    @Test
    fun parseDeepLink_cases() {
        val validNotice = parseDeepLink("notice", "123e4567-e89b-12d3-a456-426614174000")
        assertEquals(DeepLink(PushType.NOTICE, "123e4567-e89b-12d3-a456-426614174000"), validNotice)

        val validEvent = parseDeepLink("event", "223e4567-e89b-12d3-a456-426614174000")
        assertEquals(DeepLink(PushType.EVENT, "223e4567-e89b-12d3-a456-426614174000"), validEvent)

        assertNull(parseDeepLink(null, "123e4567-e89b-12d3-a456-426614174000"))
        assertNull(parseDeepLink("notice", null))
        assertNull(parseDeepLink("notice", "not-a-uuid"))
        assertNull(parseDeepLink("bad_type", "123e4567-e89b-12d3-a456-426614174000"))
        assertNull(parseDeepLink("NOTICE", "123e4567-e89b-12d3-a456-426614174000"))

        assertEquals("push_type", EXTRA_PUSH_TYPE)
        assertEquals("push_id", EXTRA_PUSH_ID)
    }
}

