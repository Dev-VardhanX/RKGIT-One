package com.rkgit.one

import com.rkgit.one.push.NotificationSettings
import com.rkgit.one.push.PermissionState
import com.rkgit.one.push.ToggleAction
import com.rkgit.one.push.isAllowed
import com.rkgit.one.push.permissionState
import com.rkgit.one.push.settingsAfterPermissionResult
import com.rkgit.one.push.shouldShowFirstPrompt
import com.rkgit.one.push.toggleAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionLogicTest {

    @Test
    fun `permissionState for SDK below 33 with notifications enabled is NOT_REQUIRED_ENABLED`() {
        for (sdk in listOf(26, 28, 30, 32)) {
            assertEquals(
                PermissionState.NOT_REQUIRED_ENABLED,
                permissionState(
                    sdkInt = sdk,
                    permissionGranted = false,
                    notificationsEnabled = true,
                    shouldShowRationale = false,
                    askedBefore = false
                )
            )
            assertEquals(
                PermissionState.NOT_REQUIRED_ENABLED,
                permissionState(
                    sdkInt = sdk,
                    permissionGranted = true,
                    notificationsEnabled = true,
                    shouldShowRationale = true,
                    askedBefore = true
                )
            )
        }
    }

    @Test
    fun `permissionState for SDK below 33 with notifications disabled is BLOCKED`() {
        for (sdk in listOf(26, 28, 30, 32)) {
            assertEquals(
                PermissionState.BLOCKED,
                permissionState(
                    sdkInt = sdk,
                    permissionGranted = true,
                    notificationsEnabled = false,
                    shouldShowRationale = false,
                    askedBefore = false
                )
            )
        }
    }

    @Test
    fun `permissionState for SDK 33 and above when granted and enabled is GRANTED`() {
        for (sdk in listOf(33, 34, 35, 36)) {
            assertEquals(
                PermissionState.GRANTED,
                permissionState(
                    sdkInt = sdk,
                    permissionGranted = true,
                    notificationsEnabled = true,
                    shouldShowRationale = false,
                    askedBefore = true
                )
            )
        }
    }

    @Test
    fun `permissionState for SDK 33 and above when granted and disabled is BLOCKED`() {
        for (sdk in listOf(33, 34, 35, 36)) {
            assertEquals(
                PermissionState.BLOCKED,
                permissionState(
                    sdkInt = sdk,
                    permissionGranted = true,
                    notificationsEnabled = false,
                    shouldShowRationale = false,
                    askedBefore = true
                )
            )
        }
    }

    @Test
    fun `permissionState for SDK 33 and above when not granted and never asked is NEEDS_REQUEST`() {
        for (sdk in listOf(33, 34, 35, 36)) {
            assertEquals(
                PermissionState.NEEDS_REQUEST,
                permissionState(
                    sdkInt = sdk,
                    permissionGranted = false,
                    notificationsEnabled = true,
                    shouldShowRationale = false,
                    askedBefore = false
                )
            )
        }
    }

    @Test
    fun `permissionState for SDK 33 and above when not granted and rationale true is NEEDS_REQUEST`() {
        for (sdk in listOf(33, 34, 35, 36)) {
            // Asked before and rationale true -> NEEDS_REQUEST
            assertEquals(
                PermissionState.NEEDS_REQUEST,
                permissionState(
                    sdkInt = sdk,
                    permissionGranted = false,
                    notificationsEnabled = true,
                    shouldShowRationale = true,
                    askedBefore = true
                )
            )
            // Never asked and rationale true -> NEEDS_REQUEST
            assertEquals(
                PermissionState.NEEDS_REQUEST,
                permissionState(
                    sdkInt = sdk,
                    permissionGranted = false,
                    notificationsEnabled = true,
                    shouldShowRationale = true,
                    askedBefore = false
                )
            )
        }
    }

    @Test
    fun `permissionState for SDK 33 and above when not granted and asked before and rationale false is BLOCKED`() {
        for (sdk in listOf(33, 34, 35, 36)) {
            assertEquals(
                PermissionState.BLOCKED,
                permissionState(
                    sdkInt = sdk,
                    permissionGranted = false,
                    notificationsEnabled = true,
                    shouldShowRationale = false,
                    askedBefore = true
                )
            )
        }
    }

    @Test
    fun `isAllowed is true only for GRANTED and NOT_REQUIRED_ENABLED`() {
        assertTrue(isAllowed(PermissionState.GRANTED))
        assertTrue(isAllowed(PermissionState.NOT_REQUIRED_ENABLED))
        assertFalse(isAllowed(PermissionState.NEEDS_REQUEST))
        assertFalse(isAllowed(PermissionState.BLOCKED))
    }

    @Test
    fun `toggleAction turning OFF is always APPLY`() {
        for (state in PermissionState.values()) {
            assertEquals(ToggleAction.APPLY, toggleAction(turnOn = false, state = state))
        }
    }

    @Test
    fun `toggleAction turning ON matches state`() {
        assertEquals(ToggleAction.APPLY, toggleAction(turnOn = true, state = PermissionState.GRANTED))
        assertEquals(ToggleAction.APPLY, toggleAction(turnOn = true, state = PermissionState.NOT_REQUIRED_ENABLED))
        assertEquals(ToggleAction.REQUEST_PERMISSION, toggleAction(turnOn = true, state = PermissionState.NEEDS_REQUEST))
        assertEquals(ToggleAction.SHOW_BLOCKED, toggleAction(turnOn = true, state = PermissionState.BLOCKED))
    }

    @Test
    fun `shouldShowFirstPrompt is true only when setupDone and not promptShown and NEEDS_REQUEST`() {
        val states = PermissionState.values()
        val bools = listOf(true, false)

        for (setup in bools) {
            for (prompt in bools) {
                for (state in states) {
                    val expected = setup && !prompt && state == PermissionState.NEEDS_REQUEST
                    assertEquals(
                        "Failed for setup=$setup, prompt=$prompt, state=$state",
                        expected,
                        shouldShowFirstPrompt(setupDone = setup, promptShown = prompt, state = state)
                    )
                }
            }
        }
    }

    @Test
    fun `settingsAfterPermissionResult when granted with requestedNotices true turns notices on and keeps events`() {
        val current = NotificationSettings(noticesEnabled = false, eventsEnabled = true)
        val result = settingsAfterPermissionResult(
            granted = true,
            current = current,
            requestedNotices = true,
            requestedEvents = false
        )
        assertEquals(NotificationSettings(noticesEnabled = true, eventsEnabled = true), result)

        val currentBothFalse = NotificationSettings(noticesEnabled = false, eventsEnabled = false)
        val result2 = settingsAfterPermissionResult(
            granted = true,
            current = currentBothFalse,
            requestedNotices = true,
            requestedEvents = false
        )
        assertEquals(NotificationSettings(noticesEnabled = true, eventsEnabled = false), result2)
    }

    @Test
    fun `settingsAfterPermissionResult when granted with both requested turns both on`() {
        val current = NotificationSettings(noticesEnabled = false, eventsEnabled = false)
        val result = settingsAfterPermissionResult(
            granted = true,
            current = current,
            requestedNotices = true,
            requestedEvents = true
        )
        assertEquals(NotificationSettings(noticesEnabled = true, eventsEnabled = true), result)
    }

    @Test
    fun `settingsAfterPermissionResult when granted with nothing requested keeps current unchanged`() {
        val current = NotificationSettings(noticesEnabled = false, eventsEnabled = true)
        val result = settingsAfterPermissionResult(
            granted = true,
            current = current,
            requestedNotices = false,
            requestedEvents = false
        )
        assertEquals(current, result)
    }

    @Test
    fun `settingsAfterPermissionResult when not granted turns both off regardless of requested`() {
        val current = NotificationSettings(noticesEnabled = true, eventsEnabled = true)

        val result1 = settingsAfterPermissionResult(
            granted = false,
            current = current,
            requestedNotices = true,
            requestedEvents = false
        )
        assertEquals(NotificationSettings(noticesEnabled = false, eventsEnabled = false), result1)

        val result2 = settingsAfterPermissionResult(
            granted = false,
            current = current,
            requestedNotices = true,
            requestedEvents = true
        )
        assertEquals(NotificationSettings(noticesEnabled = false, eventsEnabled = false), result2)

        val result3 = settingsAfterPermissionResult(
            granted = false,
            current = current,
            requestedNotices = false,
            requestedEvents = false
        )
        assertEquals(NotificationSettings(noticesEnabled = false, eventsEnabled = false), result3)
    }
}

