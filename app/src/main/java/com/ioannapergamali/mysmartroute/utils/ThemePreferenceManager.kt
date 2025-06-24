package com.ioannapergamali.mysmartroute.utils

import android.content.Context
import android.content.res.Configuration
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.ioannapergamali.mysmartroute.view.ui.AppTheme
import com.ioannapergamali.mysmartroute.data.CustomTheme
import com.ioannapergamali.mysmartroute.model.interfaces.ThemeOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

object ThemePreferenceManager {
    private val THEME_KEY = stringPreferencesKey("theme")
    private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")

    private fun encode(theme: ThemeOption): String {
        val color = String.format("#%08X", theme.seed.toArgb())
        return "${theme.label}|$color"
    }

    private fun decode(value: String): ThemeOption {
        val parts = value.split('|')
        if (parts.size == 2) {
            val label = parts[0]
            val color = try {
                Color(android.graphics.Color.parseColor(parts[1]))
            } catch (_: Exception) {
                Color(0xFF2196F3)
            }
            AppTheme.values().firstOrNull { it.label == label }?.let { return it }
            return CustomTheme(label, color)
        }
        return AppTheme.Ocean
    }

    private fun Context.isSystemDarkTheme(): Boolean {
        val mode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return mode == Configuration.UI_MODE_NIGHT_YES
    }

    fun themeFlow(context: Context): Flow<ThemeOption> =
        context.dataStore.data.map { prefs ->
            prefs[THEME_KEY]?.let { decode(it) } ?: AppTheme.Ocean
        }

    fun darkThemeFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { prefs ->
            prefs[DARK_MODE_KEY] ?: context.isSystemDarkTheme()
        }

    suspend fun setTheme(context: Context, theme: ThemeOption) {
        context.dataStore.edit { prefs ->
            prefs[THEME_KEY] = encode(theme)
        }
    }

    suspend fun setDarkTheme(context: Context, dark: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[DARK_MODE_KEY] = dark
        }
    }
}
