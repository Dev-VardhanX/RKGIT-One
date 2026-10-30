package com.rkgit.one

import com.rkgit.one.push.NotificationSettings
import com.rkgit.one.push.desiredTopics
import com.rkgit.one.push.diffTopics
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Locale

class TopicLogicTest {

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
    fun desiredTopics_bothSwitchesOnYear2It() {
        val settings = NotificationSettings(noticesEnabled = true, eventsEnabled = true)
        val topicsDev = desiredTopics(settings, year = 2, departmentCode = "it", activeDepartmentCodes = emptyList(), prefix = "dev-")
        assertEquals(setOf("dev-n-all", "dev-n-y2-it", "dev-e-all"), topicsDev)

        val topicsNoPrefix = desiredTopics(settings, year = 2, departmentCode = "it", activeDepartmentCodes = emptyList(), prefix = "")
        assertEquals(setOf("n-all", "n-y2-it", "e-all"), topicsNoPrefix)
    }

    @Test
    fun desiredTopics_switchesToggled() {
        val noticesOff = NotificationSettings(noticesEnabled = false, eventsEnabled = true)
        val topicsNoticesOff = desiredTopics(noticesOff, year = 2, departmentCode = "it", activeDepartmentCodes = emptyList(), prefix = "dev-")
        assertEquals(setOf("dev-e-all"), topicsNoticesOff)

        val eventsOff = NotificationSettings(noticesEnabled = true, eventsEnabled = false)
        val topicsEventsOff = desiredTopics(eventsOff, year = 2, departmentCode = "it", activeDepartmentCodes = emptyList(), prefix = "dev-")
        assertEquals(setOf("dev-n-all", "dev-n-y2-it"), topicsEventsOff)

        val bothOff = NotificationSettings(noticesEnabled = false, eventsEnabled = false)
        val topicsBothOff = desiredTopics(bothOff, year = 2, departmentCode = "it", activeDepartmentCodes = emptyList(), prefix = "dev-")
        assertEquals(emptySet<String>(), topicsBothOff)
    }

    @Test
    fun desiredTopics_onlyYearSet() {
        val settings = NotificationSettings(noticesEnabled = true, eventsEnabled = true)
        val activeCodes = listOf("cse", "it", "me")
        val topics = desiredTopics(settings, year = 3, departmentCode = null, activeDepartmentCodes = activeCodes, prefix = "")
        assertEquals(
            setOf("n-all", "n-y3-cse", "n-y3-it", "n-y3-me", "e-all"),
            topics
        )
    }

    @Test
    fun desiredTopics_onlyDepartmentSet() {
        val settings = NotificationSettings(noticesEnabled = true, eventsEnabled = false)
        val topics = desiredTopics(settings, year = null, departmentCode = "cse-aiml", activeDepartmentCodes = emptyList(), prefix = "")
        assertEquals(
            setOf("n-all", "n-y1-cse-aiml", "n-y2-cse-aiml", "n-y3-cse-aiml", "n-y4-cse-aiml"),
            topics
        )
    }

    @Test
    fun desiredTopics_nothingSet() {
        val settings = NotificationSettings(noticesEnabled = true, eventsEnabled = true)
        val topics = desiredTopics(settings, year = null, departmentCode = null, activeDepartmentCodes = emptyList(), prefix = "")
        assertEquals(setOf("n-all", "e-all"), topics)
    }

    @Test
    fun desiredTopics_invalidYearAndDepartmentTreatedAsNotSet() {
        val settings = NotificationSettings(noticesEnabled = true, eventsEnabled = true)

        // Invalid year 0 or 5 with null department -> nothing set -> n-all + e-all
        val topicsYear0 = desiredTopics(settings, year = 0, departmentCode = null, activeDepartmentCodes = emptyList(), prefix = "")
        assertEquals(setOf("n-all", "e-all"), topicsYear0)

        val topicsYear5 = desiredTopics(settings, year = 5, departmentCode = null, activeDepartmentCodes = emptyList(), prefix = "")
        assertEquals(setOf("n-all", "e-all"), topicsYear5)

        // Upper-case department or invalid pattern
        val topicsUpperDept = desiredTopics(settings, year = null, departmentCode = "IT", activeDepartmentCodes = emptyList(), prefix = "")
        assertEquals(setOf("n-all", "e-all"), topicsUpperDept)

        val topicsBadCode = desiredTopics(settings, year = null, departmentCode = "bad code", activeDepartmentCodes = emptyList(), prefix = "")
        assertEquals(setOf("n-all", "e-all"), topicsBadCode)
    }

    @Test
    fun desiredTopics_activeCodesSanitisedAndPatternValidated() {
        val settings = NotificationSettings(noticesEnabled = true, eventsEnabled = false)
        val activeCodes = listOf("cse", "cse", "INVALID", "it", "toolongdepartmentcodethatexceedstwenty", "me")
        val topics = desiredTopics(settings, year = 1, departmentCode = null, activeDepartmentCodes = activeCodes, prefix = "dev-")

        assertEquals(
            setOf("dev-n-all", "dev-n-y1-cse", "dev-n-y1-it", "dev-n-y1-me"),
            topics
        )

        val topicRegex = Regex("^[a-zA-Z0-9\\-_.~%]{1,200}$")
        for (topic in topics) {
            assertTrue("Topic $topic should match regex", topicRegex.matches(topic))
        }
    }

    @Test
    fun diffTopics_cases() {
        // empty subscribed -> everything to subscribe
        val diffEmptySubscribed = diffTopics(
            desired = setOf("dev-n-all", "dev-e-all"),
            subscribed = emptySet()
        )
        assertEquals(setOf("dev-n-all", "dev-e-all"), diffEmptySubscribed.toSubscribe)
        assertEquals(emptySet<String>(), diffEmptySubscribed.toUnsubscribe)

        // identical sets -> both empty
        val diffIdentical = diffTopics(
            desired = setOf("dev-n-all", "dev-e-all"),
            subscribed = setOf("dev-n-all", "dev-e-all")
        )
        assertEquals(emptySet<String>(), diffIdentical.toSubscribe)
        assertEquals(emptySet<String>(), diffIdentical.toUnsubscribe)

        // changed profile -> old y-topic in toUnsubscribe, new in toSubscribe
        val diffChanged = diffTopics(
            desired = setOf("dev-n-all", "dev-n-y3-me", "dev-e-all"),
            subscribed = setOf("dev-n-all", "dev-n-y2-it", "dev-e-all")
        )
        assertEquals(setOf("dev-n-y3-me"), diffChanged.toSubscribe)
        assertEquals(setOf("dev-n-y2-it"), diffChanged.toUnsubscribe)

        // The two result sets never overlap
        assertTrue(diffChanged.toSubscribe.intersect(diffChanged.toUnsubscribe).isEmpty())
    }
}

