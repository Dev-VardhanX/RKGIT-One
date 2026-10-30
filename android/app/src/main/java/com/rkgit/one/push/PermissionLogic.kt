package com.rkgit.one.push

enum class PermissionState {
    NOT_REQUIRED_ENABLED,
    GRANTED,
    NEEDS_REQUEST,
    BLOCKED
}

fun permissionState(
    sdkInt: Int,
    permissionGranted: Boolean,
    notificationsEnabled: Boolean,
    shouldShowRationale: Boolean,
    askedBefore: Boolean
): PermissionState {
    return if (sdkInt < 33) {
        if (notificationsEnabled) PermissionState.NOT_REQUIRED_ENABLED else PermissionState.BLOCKED
    } else {
        if (permissionGranted) {
            if (notificationsEnabled) PermissionState.GRANTED else PermissionState.BLOCKED
        } else {
            if (!askedBefore || shouldShowRationale) {
                PermissionState.NEEDS_REQUEST
            } else {
                PermissionState.BLOCKED
            }
        }
    }
}

fun isAllowed(state: PermissionState): Boolean =
    state == PermissionState.GRANTED || state == PermissionState.NOT_REQUIRED_ENABLED

enum class ToggleAction {
    APPLY,
    REQUEST_PERMISSION,
    SHOW_BLOCKED
}

fun toggleAction(turnOn: Boolean, state: PermissionState): ToggleAction {
    if (!turnOn) return ToggleAction.APPLY
    return when {
        isAllowed(state) -> ToggleAction.APPLY
        state == PermissionState.NEEDS_REQUEST -> ToggleAction.REQUEST_PERMISSION
        else -> ToggleAction.SHOW_BLOCKED
    }
}

fun shouldShowFirstPrompt(
    setupDone: Boolean,
    promptShown: Boolean,
    state: PermissionState
): Boolean = setupDone && !promptShown && state == PermissionState.NEEDS_REQUEST

fun settingsAfterPermissionResult(
    granted: Boolean,
    current: NotificationSettings,
    requestedNotices: Boolean,
    requestedEvents: Boolean
): NotificationSettings {
    if (!granted) {
        return current.copy(noticesEnabled = false, eventsEnabled = false)
    }
    return current.copy(
        noticesEnabled = if (requestedNotices) true else current.noticesEnabled,
        eventsEnabled = if (requestedEvents) true else current.eventsEnabled
    )
}

