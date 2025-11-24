package fr.enssat.sharemybook.mitosbooking.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.core.content.edit

/**
 * Gestionnaire du thème de l'application
 * Permet de basculer entre mode clair et sombre
 */
object ThemeManager {
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_DARK_THEME = "dark_theme"

    private val _isDarkTheme = MutableStateFlow<Boolean?>(null)
    val isDarkTheme: StateFlow<Boolean?> = _isDarkTheme

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _isDarkTheme.value = prefs.getBoolean(KEY_DARK_THEME, false)
    }

    fun toggleTheme(context: Context) {
        val newValue = !(_isDarkTheme.value ?: false)
        _isDarkTheme.value = newValue
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit {
                putBoolean(KEY_DARK_THEME, newValue)
            }
    }

    fun setTheme(context: Context, isDark: Boolean) {
        _isDarkTheme.value = isDark
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit {
                putBoolean(KEY_DARK_THEME, isDark)
            }
    }
}

