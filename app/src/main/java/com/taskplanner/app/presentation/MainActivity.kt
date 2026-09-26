package com.taskplanner.app.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.core.view.WindowCompat
import com.taskplanner.app.ui.theme.TaskPlannerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PlannerRoot(this) }
    }
}

@Composable
private fun PlannerRoot(activity: ComponentActivity) {
    val settings: SettingsViewModel = hiltViewModel()
    val darkMode by settings.darkMode.collectAsStateWithLifecycle()
    SideEffect {
        val insets = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        insets.isAppearanceLightStatusBars = !darkMode
        insets.isAppearanceLightNavigationBars = !darkMode
    }
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val mainRoutes = setOf("tasks", "search", "settings")

    TaskPlannerTheme(darkMode) {
        Scaffold(
            bottomBar = {
                if (route in mainRoutes) NavigationBar {
                    listOf(
                        Triple("tasks", "Задачи", Icons.Default.CheckCircle),
                        Triple("search", "Поиск", Icons.Default.Search),
                        Triple("settings", "Настройки", Icons.Default.Settings),
                    ).forEach { (destination, label, icon) ->
                        NavigationBarItem(
                            selected = route == destination,
                            onClick = {
                                if (route != destination) nav.navigate(destination) {
                                    popUpTo("tasks") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(icon, contentDescription = null) },
                            label = { Text(label) },
                        )
                    }
                }
            }
        ) { padding ->
            NavHost(navController = nav, startDestination = "tasks", modifier = Modifier.padding(padding)) {
                composable("tasks") {
                    val vm: TasksViewModel = hiltViewModel()
                    TasksScreen(
                        viewModel = vm,
                        onOpen = { nav.navigate("detail/$it") },
                        onCreate = { nav.navigate("editor") },
                        onSearch = { nav.navigate("search") },
                        onLogin = { nav.navigate("login") },
                    )
                }
                composable("search") {
                    val vm: SearchViewModel = hiltViewModel()
                    SearchScreen(vm, onOpen = { nav.navigate("detail/$it") }, onLogin = { nav.navigate("login") })
                }
                composable("settings") {
                    SettingsScreen(settings, onLogin = { nav.navigate("login") })
                }
                composable("detail/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                    val vm: DetailViewModel = hiltViewModel()
                    DetailScreen(vm, onBack = { nav.popBackStack() }, onEdit = { nav.navigate("editor?taskId=$it") })
                }
                composable(
                    "editor?taskId={taskId}",
                    arguments = listOf(navArgument("taskId") { type = NavType.StringType; defaultValue = "" })
                ) {
                    val vm: EditorViewModel = hiltViewModel()
                    EditorScreen(vm, onBack = { nav.popBackStack() })
                }
                composable("login") { LoginPreviewScreen(onBack = { nav.popBackStack() }) }
            }
        }
    }
}
