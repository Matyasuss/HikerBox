package cz.matyasuss.hikerbox.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Map : Screen("map")
    object Settings : Screen("settings")
}