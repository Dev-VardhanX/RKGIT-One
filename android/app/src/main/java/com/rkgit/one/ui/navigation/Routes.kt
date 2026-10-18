package com.rkgit.one.ui.navigation

import kotlinx.serialization.Serializable

@Serializable object HomeRoute
@Serializable object NotesRoute
@Serializable object EventsRoute
@Serializable object NoticesRoute
@Serializable data class EventDetailRoute(val id: String)
@Serializable data class SubjectsRoute(val year: Int, val semester: Int)
@Serializable data class MaterialListRoute(val subjectId: String, val subjectName: String)
@Serializable data class NoticeDetailRoute(val id: String)
@Serializable data class ViewerRoute(
    val kind: String,
    val itemId: String,
    val title: String,
    val fileName: String,
    val fileType: String,
    val fileSize: Long,
    val remotePath: String
)
