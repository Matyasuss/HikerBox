package cz.matyasuss.hikerbox.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
    object Map : Screen("map")
    object Settings : Screen("settings")
    object Detail : Screen("detail/{chargerId}") {
        fun createRoute(chargerId: String) = "detail/$chargerId"
    }
}