package com.rkgit.one.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rkgit.one.ui.components.ScreenScaffold
import com.rkgit.one.ui.navigation.EventDetailRoute
import com.rkgit.one.ui.navigation.EventsRoute
import com.rkgit.one.ui.navigation.HomeRoute
import com.rkgit.one.ui.navigation.NoticesRoute
import com.rkgit.one.ui.navigation.NotesRoute
import com.rkgit.one.ui.screens.events.EventDetailScreen
import com.rkgit.one.ui.screens.events.EventsScreen
import com.rkgit.one.ui.screens.home.HomeScreen
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
    TopLevelItem("Notes", NotesRoute, Icons.Outlined.Description),
    TopLevelItem("Events", EventsRoute, Icons.Outlined.Event, listOf(EventDetailRoute::class)),
    TopLevelItem("Notices", NoticesRoute, Icons.Outlined.Notifications)
)

@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar {
                topLevelItems.forEach { item ->
                    NavigationBarItem(
                        selected = destination?.hierarchy?.any { d ->
                            d.hasRoute(item.route::class) || item.related.any { d.hasRoute(it) }
                        } == true,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier.padding(padding)
        ) {
            composable<HomeRoute> { HomeScreen() }
            composable<NotesRoute> { Placeholder("Notes") }
            composable<EventsRoute> {
                EventsScreen(onEventClick = { id -> navController.navigate(EventDetailRoute(id)) })
            }
            composable<EventDetailRoute> {
                EventDetailScreen(onBack = { navController.popBackStack() })
            }
            composable<NoticesRoute> { Placeholder("Notices") }
        }
    }
}

@Composable
private fun Placeholder(name: String) {
    ScreenScaffold(name) { padding ->
        Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("$name (coming soon)")
        }
    }
}
