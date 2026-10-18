package com.rkgit.one.data.remote.dto

import com.rkgit.one.domain.model.StudyMaterial
import com.rkgit.one.domain.model.Subject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime

@Serializable
data class CountDto(val count: Int)

@Serializable
data class SubjectDto(
    val id: String,
    val year: Int,
    val semester: Int,
    val name: String,
    @SerialName("study_materials") val materials: List<CountDto> = emptyList()
)

fun SubjectDto.toDomain() = Subject(
    id = id,
    year = year,
    semester = semester,
    name = name,
    materialCount = materials.firstOrNull()?.count ?: 0
)

@Serializable
data class StudyMaterialDto(
    val id: String,
    @SerialName("subject_id") val subjectId: String,
    val title: String,
    val description: String? = null,
    @SerialName("file_path") val filePath: String,
    @SerialName("file_name") val fileName: String,
    @SerialName("file_type") val fileType: String,
    @SerialName("file_size") val fileSize: Long,
    @SerialName("created_at") val createdAt: String
)

fun StudyMaterialDto.toDomain() = StudyMaterial(
    id = id,
    subjectId = subjectId,
    title = title,
    description = description?.takeIf { it.isNotBlank() },
    fileName = fileName,
    filePath = filePath,
    fileType = fileType,
    fileSize = fileSize,
    createdAt = OffsetDateTime.parse(createdAt).toInstant()
)
