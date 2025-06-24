package com.ioannapergamali.mysmartroute.utils

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ioannapergamali.mysmartroute.data.CustomTheme
import com.ioannapergamali.mysmartroute.data.ThemeOption
import com.ioannapergamali.mysmartroute.view.ui.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

object ThemePreferenceManager {
    private val THEME_KEY = preferencesKey<String>("theme_name")
    private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")

    private fun Context.isSystemDarkTheme(): Boolean {
        val mode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return mode == Configuration.UI_MODE_NIGHT_YES
    }

    fun themeFlow(context: Context): Flow<ThemeOption> =
        context.dataStore.data.map { prefs ->
            val raw = prefs[THEME_KEY] ?: AppTheme.Ocean.name
            decodeTheme(raw)
        }

    fun darkThemeFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { prefs ->
            prefs[DARK_MODE_KEY] ?: context.isSystemDarkTheme()
        }

    suspend fun setTheme(context: Context, theme: ThemeOption) {
        context.dataStore.edit { prefs ->
            prefs[THEME_KEY] = encodeTheme(theme)
        }
    }

    suspend fun setDarkTheme(context: Context, dark: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[DARK_MODE_KEY] = dark
        }
    }

    fun encodeTheme(theme: ThemeOption): String = when (theme) {
        is AppTheme -> theme.name
        is CustomTheme -> "${theme.label}|${colorToHex(theme.seed)}"
        else -> AppTheme.Ocean.name
    }

    fun decodeTheme(value: String): ThemeOption =
        if ("|" in value) {
            val parts = value.split("|", limit = 2)
            val label = parts.getOrNull(0) ?: "Custom"
            val color = runCatching { Color(android.graphics.Color.parseColor(parts.getOrElse(1) { "#2196F3" })) }.getOrElse { Color(0xFF2196F3) }
            CustomTheme(label, color)
        } else {
            runCatching { AppTheme.valueOf(value) }.getOrDefault(AppTheme.Ocean)
        }

    private fun colorToHex(color: Color): String {
        val intColor = color.toArgb() and 0xFFFFFF
        return "#%06X".format(intColor)
    }
}
