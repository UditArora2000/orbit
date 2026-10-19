package com.udit.catchup.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.padding
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.udit.catchup.ui.dashboard.DashboardScreen
import com.udit.catchup.ui.detail.PersonDetailScreen
import com.udit.catchup.ui.friends.FriendsScreen
import com.udit.catchup.ui.review.ReviewScreen
import com.udit.catchup.ui.settings.SettingsScreen
import com.udit.catchup.ui.triage.TriageScreen

private data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
)

private fun NavHostController.openTab(route: String) {
    if (currentDestination?.route == route) return
    // Home is the root, so never restore a saved stack of screens above it.
    if (route == "dashboard" && popBackStack("dashboard", inclusive = false, saveState = true)) return
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = route != "dashboard"
    }
}

@Composable
fun AppRoot(initialRoute: String?) {
    val nav = rememberNavController()
    val items = listOf(
        NavItem("dashboard", "Home", Icons.Outlined.Home, Icons.Filled.Home),
        NavItem("triage", "File", Icons.Outlined.PersonSearch, Icons.Filled.PersonSearch),
        NavItem("friends", "Friends", Icons.Outlined.Group, Icons.Filled.Group),
        NavItem("settings", "Settings", Icons.Outlined.Settings, Icons.Filled.Settings),
    )
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEach { item ->
                    val selected = currentRoute == item.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = { nav.openTab(item.route) },
                        icon = {
                            Icon(
                                if (selected) item.selectedIcon else item.icon,
                                contentDescription = item.label,
                            )
                        },
                        label = { Text(item.label) },
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = "dashboard",
            modifier = Modifier.padding(padding),
        ) {
            composable("dashboard") {
                DashboardScreen(
                    onGoTriage = { nav.openTab("triage") },
                    onGoFriends = { nav.openTab("friends") },
                )
            }
            composable("triage") {
                TriageScreen(
                    onOpenSkipped = { nav.navigate("skipped") },
                    onOpenArchived = { nav.navigate("archived") },
                )
            }
            composable("skipped") {
                ReviewScreen(
                    mode = "skipped",
                    onOpenPerson = { id -> nav.navigate("person/$id") },
                    onBack = { nav.popBackStack() },
                )
            }
            composable("archived") {
                ReviewScreen(
                    mode = "archived",
                    onOpenPerson = { id -> nav.navigate("person/$id") },
                    onBack = { nav.popBackStack() },
                )
            }
            composable("friends") {
                FriendsScreen(onOpenPerson = { id -> nav.navigate("person/$id") })
            }
            composable("settings") { SettingsScreen() }
            composable(
                "person/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: return@composable
                PersonDetailScreen(personId = id, onBack = { nav.popBackStack() })
            }
        }
    }

    LaunchedEffect(initialRoute) {
        if (initialRoute != null && items.any { it.route == initialRoute }) {
            nav.openTab(initialRoute)
        }
    }
}
