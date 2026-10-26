package com.rkgit.one.data.remote.dto

import com.rkgit.one.domain.model.Notice
import com.rkgit.one.domain.model.NoticeAttachment
import com.rkgit.one.domain.model.NoticeCategory
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime

@Serializable
data class NoticeDto(
    val id: String,
    val title: String,
    val body: String? = null,
    val category: String,
    val pinned: Boolean,
    @SerialName("link_url") val linkUrl: String? = null,
    @SerialName("file_path") val filePath: String? = null,
    @SerialName("file_name") val fileName: String? = null,
    @SerialName("file_type") val fileType: String? = null,
    @SerialName("file_size") val fileSize: Long? = null,
    @SerialName("published_at") val publishedAt: String,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("target_years") val targetYears: List<Int?>? = null,
    @SerialName("target_departments") val targetDepartments: List<String?>? = null
)

fun NoticeDto.toDomain(): Notice {
    val attachment = if (!filePath.isNullOrBlank() && fileName != null && fileType != null && fileSize != null) {
        NoticeAttachment(
            path = filePath,
            fileName = fileName,
            fileType = fileType,
            fileSize = fileSize
        )
    } else {
        null
    }

    return Notice(
        id = id,
        title = title,
        body = body?.takeIf { it.isNotBlank() },
        category = NoticeCategory.fromValue(category),
        pinned = pinned,
        linkUrl = linkUrl?.takeIf { it.isNotBlank() },
        attachment = attachment,
        publishedAt = OffsetDateTime.parse(publishedAt).toInstant(),
        expiresAt = expiresAt?.takeIf { it.isNotBlank() }?.let { OffsetDateTime.parse(it).toInstant() },
        targetYears = targetYears?.filterNotNull()?.filter { it in 1..4 }?.distinct() ?: emptyList(),
        targetDepartments = targetDepartments?.filterNotNull()?.distinct() ?: emptyList()
    )
}
