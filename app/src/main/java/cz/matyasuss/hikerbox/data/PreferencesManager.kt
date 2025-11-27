package cz.matyasuss.hikerbox.data

import android.content.Context
import android.content.SharedPreferences
import cz.matyasuss.hikerbox.model.LoginMethod
import cz.matyasuss.hikerbox.model.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("hikerbox_prefs", Context.MODE_PRIVATE)

    private val _userPreferences = MutableStateFlow(loadPreferences())
    val userPreferences: StateFlow<UserPreferences> = _userPreferences.asStateFlow()

    private fun loadPreferences(): UserPreferences {
        return UserPreferences(
            isLoggedIn = prefs.getBoolean("is_logged_in", false),
            loginMethod = LoginMethod.valueOf(
                prefs.getString("login_method", LoginMethod.NONE.name) ?: LoginMethod.NONE.name
            ),
            email = prefs.getString("email", null),
            displayName = prefs.getString("display_name", null),
            showAllChargers = prefs.getBoolean("show_all_chargers", true),
            mapZoomLevel = prefs.getFloat("map_zoom_level", 7.0f).toDouble(),
            notificationsEnabled = prefs.getBoolean("notifications_enabled", true),
            darkMode = prefs.getBoolean("dark_mode", true)
        )
    }

    fun savePreferences(preferences: UserPreferences) {
        prefs.edit().apply {
            putBoolean("is_logged_in", preferences.isLoggedIn)
            putString("login_method", preferences.loginMethod.name)
            putString("email", preferences.email)
            putString("display_name", preferences.displayName)
            putBoolean("show_all_chargers", preferences.showAllChargers)
            putFloat("map_zoom_level", preferences.mapZoomLevel.toFloat())
            putBoolean("notifications_enabled", preferences.notificationsEnabled)
            putBoolean("dark_mode", preferences.darkMode)
            apply()
        }
        _userPreferences.value = preferences
    }

    fun login(method: LoginMethod, email: String, displayName: String? = null) {
        val updated = _userPreferences.value.copy(
            isLoggedIn = true,
            loginMethod = method,
            email = email,
            displayName = displayName ?: email.substringBefore("@")
        )
        savePreferences(updated)
    }

    fun logout() {
        val updated = _userPreferences.value.copy(
            isLoggedIn = false,
            loginMethod = LoginMethod.NONE,
            email = null,
            displayName = null
        )
        savePreferences(updated)
    }

    fun updateSetting(key: String, value: Any) {
        val current = _userPreferences.value
        val updated = when (key) {
            "show_all_chargers" -> current.copy(showAllChargers = value as Boolean)
            "notifications_enabled" -> current.copy(notificationsEnabled = value as Boolean)
            "dark_mode" -> current.copy(darkMode = value as Boolean)
            "map_zoom_level" -> current.copy(mapZoomLevel = value as Double)
            else -> current
        }
        savePreferences(updated)
    }
}