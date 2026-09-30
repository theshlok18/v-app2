package com.samai.assistant.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.samai.assistant.ui.screens.*

data class NavItem(val route: String, val label: String, val icon: ImageVector)

private val bottomNavItems = listOf(
    NavItem(Screen.HOME, "Home", Icons.Filled.Home),
    NavItem(Screen.CHAT, "Chat", Icons.AutoMirrored.Filled.Chat),
    NavItem(Screen.SAM_CORE, "SAM Core", Icons.Filled.AutoAwesome),
    NavItem(Screen.DISCOVER, "Discover", Icons.Filled.Explore),
    NavItem(Screen.SETTINGS, "Settings", Icons.Filled.Settings),
)

@Composable
fun SAMApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = bottomNavItems.any { item ->
        currentDestination?.hierarchy?.any { it.route == item.route } == true
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    tonalElevation = 0.dp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.HOME) { HomeScreen(navController) }
            composable(Screen.CHAT) { ChatScreen(navController) }
            composable(Screen.SAM_CORE) { SAMCoreScreen(navController) }
            composable(Screen.DISCOVER) { DiscoverScreen(navController) }
            composable(Screen.SETTINGS) { SettingsScreen(navController) }
            composable(Screen.PERSONALIZATION) { PersonalizationScreen(navController) }
            composable(Screen.AI_MODELS) { AIModelsScreen(navController) }
            composable(Screen.ORB_CUSTOMIZE) { OrbCustomizeScreen(navController) }
            composable(Screen.DEVELOPER_MODE) { DeveloperModeScreen(navController) }
            composable(Screen.TASKS) { TasksScreen(navController) }
            composable(Screen.MEMORY) { MemoryScreen(navController) }
            composable(Screen.KNOWLEDGE) { KnowledgeScreen(navController) }
            composable(Screen.CODING) { CodingScreen(navController) }
        }
    }
}
