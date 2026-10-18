package com.rkgit.one.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rkgit.one.ui.navigation.DownloadsRoute
import com.rkgit.one.ui.navigation.EventDetailRoute
import com.rkgit.one.ui.navigation.EventsRoute
import com.rkgit.one.ui.navigation.HomeRoute
import com.rkgit.one.ui.navigation.MaterialListRoute
import com.rkgit.one.ui.navigation.NoticeDetailRoute
import com.rkgit.one.ui.navigation.NoticesRoute
import com.rkgit.one.ui.navigation.NotesRoute
import com.rkgit.one.ui.navigation.SubjectsRoute
import com.rkgit.one.ui.navigation.ViewerRoute
import com.rkgit.one.ui.navigation.toViewerRoute
import com.rkgit.one.ui.screens.downloads.DownloadsScreen
import com.rkgit.one.ui.screens.events.EventDetailScreen
import com.rkgit.one.ui.screens.events.EventsScreen
import com.rkgit.one.ui.screens.home.HomeScreen
import com.rkgit.one.ui.screens.materials.MaterialListScreen
import com.rkgit.one.ui.screens.materials.NotesScreen
import com.rkgit.one.ui.screens.materials.SubjectsScreen
import com.rkgit.one.ui.screens.notices.NoticeDetailScreen
import com.rkgit.one.ui.screens.notices.NoticesScreen
import com.rkgit.one.ui.screens.viewer.ViewerScreen
import kotlin.reflect.KClass

private data class TopLevelItem(
    val label: String,
    val route: Any,
    val icon: ImageVector,
    /** Other routes that belong to this tab, so the tab stays highlighted on them. */
    val related: List<KClass<*>> = emptyList()
)

private val topLevelItems = listOf(
    TopLevelItem("Home", HomeRoute, Icons.Outlined.Home),
    TopLevelItem("Notes", NotesRoute, Icons.Outlined.Description, listOf(SubjectsRoute::class, MaterialListRoute::class, DownloadsRoute::class)),
    TopLevelItem("Events", EventsRoute, Icons.Outlined.Event, listOf(EventDetailRoute::class)),
    TopLevelItem("Notices", NoticesRoute, Icons.Outlined.Notifications, listOf(NoticeDetailRoute::class))
)

@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (destination?.hasRoute(ViewerRoute::class) != true) {
                NavigationBar {
                    topLevelItems.forEach { item ->
                        val selected = destination?.hierarchy?.any { d ->
                            d.hasRoute(item.route::class) || item.related.any { d.hasRoute(it) }
                        } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (selected) {
                                    val popped = navController.popBackStack(item.route, inclusive = false)
                                    if (!popped) {
                                        navController.navigateToTab(item.route)
                                    }
                                } else {
                                    navController.navigateToTab(item.route)
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier.padding(padding)
        ) {
            composable<HomeRoute> {
                HomeScreen(
                    onNoticeClick = { navController.navigate(NoticeDetailRoute(it)) },
                    onEventClick = { navController.navigate(EventDetailRoute(it)) },
                    onMaterialClick = { id, name, y, s -> navController.navigate(MaterialListRoute(id, name, y, s)) },
                    onDownloadsClick = { navController.navigate(DownloadsRoute) },
                    onSeeAllNotices = { navController.navigateToTab(NoticesRoute) },
                    onSeeAllNotes = { navController.navigateToTab(NotesRoute) },
                    onSeeAllEvents = { navController.navigateToTab(EventsRoute) }
                )
            }
            composable<NotesRoute> {
                NotesScreen(
                    onSemesterClick = { y, s -> navController.navigate(SubjectsRoute(y, s)) },
                    onDownloadsClick = { navController.navigate(DownloadsRoute) }
                )
            }
            composable<SubjectsRoute> {
                SubjectsScreen(
                    onBack = { navController.popBackStack() },
                    onSubjectClick = { navController.navigate(MaterialListRoute(it.id, it.name, it.year, it.semester)) }
                )
            }
            composable<MaterialListRoute> {
                MaterialListScreen(
                    onBack = { navController.popBackStack() },
                    onMaterialClick = { route ->
                        navController.navigate(route)
                    }
                )
            }
            composable<DownloadsRoute> {
                DownloadsScreen(
                    onBack = { navController.popBackStack() },
                    onOpen = { row ->
                        navController.navigate(
                            ViewerRoute(
                                kind = row.kind,
                                itemId = row.itemId,
                                title = row.title,
                                fileName = row.fileName,
                                fileType = row.fileType,
                                fileSize = row.sizeBytes,
                                remotePath = row.remoteFilePath,
                                subtitle = row.subtitle,
                                year = row.year ?: 0,
                                semester = row.semester ?: 0
                            )
                        )
                    }
                )
            }
            composable<ViewerRoute> {
                ViewerScreen(onBack = { navController.popBackStack() })
            }
            composable<EventsRoute> {
                EventsScreen(onEventClick = { id -> navController.navigate(EventDetailRoute(id)) })
            }
            composable<EventDetailRoute> {
                EventDetailScreen(onBack = { navController.popBackStack() })
            }
            composable<NoticesRoute> {
                NoticesScreen(onNoticeClick = { id -> navController.navigate(NoticeDetailRoute(id)) })
            }
            composable<NoticeDetailRoute> {
                NoticeDetailScreen(
                    onBack = { navController.popBackStack() },
                    onOpenAttachment = { notice ->
                        notice.toViewerRoute()?.let { navController.navigate(it) }
                    }
                )
            }
        }
    }
}

private fun NavController.navigateToTab(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
