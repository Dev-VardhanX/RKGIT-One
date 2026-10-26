package com.rkgit.one.data.remote.dto

import com.rkgit.one.domain.model.Department
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DepartmentDto(
    val id: String,
    val code: String,
    val name: String,
    @SerialName("sort_order") val sortOrder: Int = 0,
    val active: Boolean = true
)

fun DepartmentDto.toDomain(): Department = Department(
    id = id,
    code = code,
    name = name,
    sortOrder = sortOrder,
    active = active
)
