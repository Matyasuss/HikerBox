package cz.matyasuss.hikerbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cz.matyasuss.hikerbox.data.PreferencesManager
import cz.matyasuss.hikerbox.ui.navigation.Screen
import cz.matyasuss.hikerbox.ui.screen.DetailScreen
import cz.matyasuss.hikerbox.ui.screen.HomeScreen
import cz.matyasuss.hikerbox.ui.screen.MapScreen
import cz.matyasuss.hikerbox.ui.screen.SettingsScreen
import cz.matyasuss.hikerbox.ui.theme.HikerBoxTheme

class MainActivity : ComponentActivity() {
    private lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        preferencesManager = PreferencesManager(this)

        enableEdgeToEdge()
        setContent {
            val preferences by preferencesManager.userPreferences.collectAsState()
            val isDarkTheme = preferences.darkMode

            HikerBoxTheme(darkTheme = isDarkTheme) {
                MainScreen(
                    preferencesManager = preferencesManager,
                    isDarkTheme = isDarkTheme
                )
            }
        }
    }
}

@Composable
fun MainScreen(
    preferencesManager: PreferencesManager,
    isDarkTheme: Boolean
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute?.startsWith("detail/") != true

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    BottomNavItem.entries.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title) },
                            selected = currentRoute == item.route,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(Screen.Home.route) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Home.route) {
                HomeScreen()
            }
            composable(Screen.Map.route) {
                MapScreen(
                    onChargerClick = { chargerId ->
                        navController.navigate(Screen.Detail.createRoute(chargerId))
                    }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    preferencesManager = preferencesManager,
                    onThemeChange = { /* Theme se změní automaticky přes state */ }
                )
            }
            composable(
                route = Screen.Detail.route,
                arguments = listOf(
                    navArgument("chargerId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val chargerId = backStackEntry.arguments?.getString("chargerId") ?: ""
                DetailScreen(
                    chargerId = chargerId,
                    onNavigateBack = { navController.navigateUp() }
                )
            }
        }
    }
}

enum class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val title: String
) {
    HOME(Screen.Home.route, Icons.Default.Home, "Domů"),
    MAP(Screen.Map.route, Icons.Default.Place, "Mapa"),
    SETTINGS(Screen.Settings.route, Icons.Default.Settings, "Nastavení")
}