package com.rkgit.one.push

import com.rkgit.one.domain.model.NoticeCategory
import java.time.Instant
import java.time.OffsetDateTime

enum class PushType {
    NOTICE,
    EVENT
}

data class PushMessage(
    val type: PushType,
    val id: String,
    val title: String,
    val category: String?,
    val eventDate: String?,
    val venue: String?
)

private val UUID_REGEX = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

fun parsePush(data: Map<String, String>): PushMessage? {
    val rawType = data["type"] ?: return null
    val type = when (rawType) {
        "notice" -> PushType.NOTICE
        "event" -> PushType.EVENT
        else -> return null
    }

    val rawId = data["id"] ?: return null
    if (!UUID_REGEX.matches(rawId)) return null

    val rawTitle = data["title"] ?: return null
    val trimmedTitle = rawTitle.trim()
    if (trimmedTitle.isEmpty()) return null

    val title = if (trimmedTitle.codePointCount(0, trimmedTitle.length) > 200) {
        val end = trimmedTitle.offsetByCodePoints(0, 200)
        trimmedTitle.substring(0, end)
    } else {
        trimmedTitle
    }

    val category = data["category"]?.trim()?.takeIf { it.isNotEmpty() }
    val eventDate = data["event_date"]?.trim()?.takeIf { it.isNotEmpty() }
    val venue = data["venue"]?.trim()?.takeIf { it.isNotEmpty() }

    return PushMessage(
        type = type,
        id = rawId,
        title = title,
        category = category,
        eventDate = eventDate,
        venue = venue
    )
}

data class NotificationText(val title: String, val body: String)

fun notificationText(msg: PushMessage, formatDate: (Instant) -> String): NotificationText {
    return when (msg.type) {
        PushType.NOTICE -> {
            val title = if (!msg.category.isNullOrBlank()) {
                "New notice · ${NoticeCategory.fromValue(msg.category).label}"
            } else {
                "New notice"
            }
            NotificationText(title = title, body = msg.title)
        }
        PushType.EVENT -> {
            val title = "New event"
            val parsedInstant = if (!msg.eventDate.isNullOrBlank()) {
                try {
                    OffsetDateTime.parse(msg.eventDate).toInstant()
                } catch (_: Throwable) {
                    null
                }
            } else {
                null
            }

            val body = if (parsedInstant != null) {
                "${msg.title} · ${formatDate(parsedInstant)}"
            } else {
                msg.title
            }
            NotificationText(title = title, body = body)
        }
    }
}

fun notificationId(msg: PushMessage): Int = "${msg.type}:${msg.id}".hashCode()

const val EXTRA_PUSH_TYPE = "push_type"
const val EXTRA_PUSH_ID = "push_id"

data class DeepLink(val type: PushType, val id: String)

fun parseDeepLink(type: String?, id: String?): DeepLink? {
    if (type == null || id == null) return null
    val pushType = when (type) {
        "notice" -> PushType.NOTICE
        "event" -> PushType.EVENT
        else -> return null
    }
    if (!UUID_REGEX.matches(id)) return null
    return DeepLink(type = pushType, id = id)
}

