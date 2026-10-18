package com.rkgit.one.domain.model

data class RecentMaterial(
    val material: StudyMaterial,
    val subjectName: String,
    val year: Int,
    val semester: Int
)
