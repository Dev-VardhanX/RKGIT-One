package com.rkgit.one.data.remote.dto

import com.rkgit.one.domain.model.MaterialType
import com.rkgit.one.domain.model.NoticeCategory
import com.rkgit.one.domain.model.SearchEvent
import com.rkgit.one.domain.model.SearchMaterial
import com.rkgit.one.domain.model.SearchNotice
import com.rkgit.one.domain.model.SearchResults
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import java.time.OffsetDateTime

@Serializable
data class SearchRowDto(
    val kind: String,
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val snippet: String? = null,
    val rank: Int = 0,
    @SerialName("sort_date") val sortDate: String? = null,
    val extra: JsonObject = JsonObject(emptyMap())
)

@Serializable
private data class MaterialExtraDto(
    @SerialName("subject_id") val subjectId: String = "",
    @SerialName("subject_name") val subjectName: String = "",
    val year: Int = 0,
    val semester: Int = 0,
    @SerialName("file_path") val filePath: String = "",
    @SerialName("file_name") val fileName: String = "",
    @SerialName("file_type") val fileType: String = "",
    @SerialName("file_size") val fileSize: Long = 0L,
    @SerialName("material_type") val materialType: String? = "notes"
)

@Serializable
private data class EventExtraDto(
    @SerialName("event_date") val eventDate: String? = null,
    val venue: String? = null
)

@Serializable
private data class NoticeExtraDto(
    val category: String = "",
    val pinned: Boolean = false,
    @SerialName("has_attachment") val hasAttachment: Boolean = false
)

fun List<SearchRowDto>.toSearchResults(json: Json): SearchResults {
    val materials = mutableListOf<SearchMaterial>()
    val events = mutableListOf<SearchEvent>()
    val notices = mutableListOf<SearchNotice>()

    for (row in this) {
        try {
            val cleanSnippet = row.snippet?.takeIf { it.isNotBlank() }
            when (row.kind) {
                "material" -> {
                    val extra = json.decodeFromJsonElement<MaterialExtraDto>(row.extra)
                    materials.add(
                        SearchMaterial(
                            id = row.id,
                            title = row.title,
                            subjectId = extra.subjectId,
                            subjectName = extra.subjectName.ifBlank { row.subtitle.orEmpty() },
                            year = extra.year,
                            semester = extra.semester,
                            snippet = cleanSnippet,
                            filePath = extra.filePath,
                            fileName = extra.fileName,
                            fileType = extra.fileType,
                            fileSize = extra.fileSize,
                            type = MaterialType.fromValue(extra.materialType)
                        )
                    )
                }
                "event" -> {
                    val extra = json.decodeFromJsonElement<EventExtraDto>(row.extra)
                    val dateString = extra.eventDate ?: row.sortDate ?: continue
                    val instant = OffsetDateTime.parse(dateString).toInstant()
                    val cleanVenue = extra.venue?.takeIf { it.isNotBlank() } ?: row.subtitle?.takeIf { it.isNotBlank() }
                    events.add(
                        SearchEvent(
                            id = row.id,
                            title = row.title,
                            venue = cleanVenue,
                            snippet = cleanSnippet,
                            eventDate = instant
                        )
                    )
                }
                "notice" -> {
                    val extra = json.decodeFromJsonElement<NoticeExtraDto>(row.extra)
                    val category = NoticeCategory.fromValue(extra.category)
                    notices.add(
                        SearchNotice(
                            id = row.id,
                            title = row.title,
                            category = category,
                            pinned = extra.pinned,
                            hasAttachment = extra.hasAttachment,
                            snippet = cleanSnippet
                        )
                    )
                }
                else -> {
                    // ignore unknown kinds
                }
            }
        } catch (_: Throwable) {
            // ignore rows whose extra cannot be decoded (never throw)
        }
    }

    return SearchResults(
        materials = materials,
        events = events,
        notices = notices
    )
}
