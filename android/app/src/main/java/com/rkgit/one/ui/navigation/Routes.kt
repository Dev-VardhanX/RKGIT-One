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
