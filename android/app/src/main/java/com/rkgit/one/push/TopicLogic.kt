package com.rkgit.one.push

data class NotificationSettings(
    val noticesEnabled: Boolean = true,
    val eventsEnabled: Boolean = true
)

private val TOPIC_PATTERN = Regex("^[a-zA-Z0-9\\-_.~%]{1,200}$")
private val DEPT_CODE_PATTERN = Regex("^[a-z0-9-]{2,20}$")

fun desiredTopics(
    settings: NotificationSettings,
    year: Int?,
    departmentCode: String?,
    activeDepartmentCodes: List<String>,
    prefix: String
): Set<String> {
    val result = mutableSetOf<String>()

    if (settings.noticesEnabled) {
        result.add("${prefix}n-all")
        val validYear = year?.takeIf { it in 1..4 }
        val validDept = departmentCode?.takeIf { DEPT_CODE_PATTERN.matches(it) }
        val sanitisedCodes = activeDepartmentCodes.filter { DEPT_CODE_PATTERN.matches(it) }.distinct()

        if (validYear != null && validDept != null) {
            result.add("${prefix}n-y$validYear-$validDept")
        } else if (validYear != null && validDept == null) {
            for (code in sanitisedCodes) {
                result.add("${prefix}n-y$validYear-$code")
            }
        } else if (validYear == null && validDept != null) {
            for (y in 1..4) {
                result.add("${prefix}n-y$y-$validDept")
            }
        }
    }

    if (settings.eventsEnabled) {
        result.add("${prefix}e-all")
    }

    return result.filter { TOPIC_PATTERN.matches(it) }.toSet()
}

data class TopicDiff(
    val toSubscribe: Set<String>,
    val toUnsubscribe: Set<String>
)

fun diffTopics(desired: Set<String>, subscribed: Set<String>): TopicDiff {
    return TopicDiff(
        toSubscribe = desired - subscribed,
        toUnsubscribe = subscribed - desired
    )
}

interface NotificationSettingsStore {
    fun read(): NotificationSettings
    fun write(s: NotificationSettings)
    fun readSubscribed(): Set<String>
    fun writeSubscribed(topics: Set<String>)
    fun readPromptShown(): Boolean
    fun writePromptShown(shown: Boolean)
    fun readPermissionRequested(): Boolean
    fun writePermissionRequested(requested: Boolean)
}

