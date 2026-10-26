package com.rkgit.one.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Department(
    val id: String,
    val code: String,
    val name: String,
    val sortOrder: Int,
    val active: Boolean
)
