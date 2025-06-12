package com.ioannapergamali.mysmartroute.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ioannapergamali.mysmartroute.view.ui.AppFont
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.fontDataStore by preferencesDataStore(name = "font_settings")

object FontPreferenceManager {
    private val FONT_KEY = intPreferencesKey("font")

    fun fontFlow(context: Context): Flow<AppFont> =
        context.fontDataStore.data.map { prefs ->
            val index = prefs[FONT_KEY] ?: 0
            AppFont.values().getOrElse(index) { AppFont.SansSerif }
        }

    suspend fun setFont(context: Context, font: AppFont) {
        context.fontDataStore.edit { prefs ->
            prefs[FONT_KEY] = font.ordinal
        }
    }
}
