package com.ioannapergamali.mysmartroute.utils

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.ioannapergamali.mysmartroute.view.ui.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

object ThemePreferenceManager {
    private val THEME_KEY = intPreferencesKey("theme")

    fun themeFlow(context: Context): Flow<AppTheme> =
        context.dataStore.data.map { prefs ->
            val index = prefs[THEME_KEY] ?: 0
            AppTheme.values().getOrElse(index) { AppTheme.Ocean }
        }

    suspend fun setTheme(context: Context, theme: AppTheme) {
        context.dataStore.edit { prefs ->
            prefs[THEME_KEY] = theme.ordinal
        }
    }
}
