package com.rkgit.one.data.remote.dto

import com.rkgit.one.domain.model.MaterialType
import com.rkgit.one.domain.model.RecentMaterial
import com.rkgit.one.domain.model.StudyMaterial
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime

@Serializable
data class SubjectInfoDto(
    val name: String,
    val year: Int,
    val semester: Int
)

@Serializable
data class RecentMaterialDto(
    val id: String,
    @SerialName("subject_id") val subjectId: String,
    val title: String,
    val description: String? = null,
    @SerialName("file_path") val filePath: String,
    @SerialName("file_name") val fileName: String,
    @SerialName("file_type") val fileType: String,
    @SerialName("file_size") val fileSize: Long,
    @SerialName("created_at") val createdAt: String,
    @SerialName("material_type") val materialType: String = "notes",
    val subjects: SubjectInfoDto
)

fun RecentMaterialDto.toDomain() = RecentMaterial(
    material = StudyMaterial(
        id = id,
        subjectId = subjectId,
        title = title,
        description = description?.takeIf { it.isNotBlank() },
        fileName = fileName,
        filePath = filePath,
        fileType = fileType,
        fileSize = fileSize,
        createdAt = OffsetDateTime.parse(createdAt).toInstant(),
        type = MaterialType.fromValue(materialType)
    ),
    subjectName = subjects.name,
    year = subjects.year,
    semester = subjects.semester
)
