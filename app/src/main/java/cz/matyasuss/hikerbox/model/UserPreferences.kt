package cz.matyasuss.hikerbox.model

data class UserPreferences(
    val isLoggedIn: Boolean = false,
    val loginMethod: LoginMethod = LoginMethod.NONE,
    val email: String? = null,
    val displayName: String? = null,
    val showAllChargers: Boolean = true,
    val mapZoomLevel: Double = 7.0,
    val notificationsEnabled: Boolean = true,
    val darkMode: Boolean = true
)

enum class LoginMethod {
    NONE,
    EMAIL,
    GOOGLE
}