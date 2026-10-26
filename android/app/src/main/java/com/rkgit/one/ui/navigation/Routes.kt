package com.rkgit.one.ui.navigation

import kotlinx.serialization.Serializable

@Serializable object HomeRoute
@Serializable object NotesRoute
@Serializable object EventsRoute
@Serializable object NoticesRoute
@Serializable data class EventDetailRoute(val id: String)
@Serializable data class SubjectsRoute(val year: Int, val semester: Int)
@Serializable data class MaterialListRoute(
    val subjectId: String,
    val subjectName: String,
    val year: Int = 0,
    val semester: Int = 0
)
@Serializable data class NoticeDetailRoute(val id: String)
@Serializable data object DownloadsRoute
@Serializable data object SearchRoute
@Serializable data object SetupRoute
@Serializable data object SettingsRoute
@Serializable data class ViewerRoute(
    val kind: String,
    val itemId: String,
    val title: String,
    val fileName: String,
    val fileType: String,
    val fileSize: Long,
    val remotePath: String,
    val subtitle: String = "",
    val year: Int = 0,
    val semester: Int = 0
)
