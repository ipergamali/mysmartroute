package com.ioannapergamali.mysmartroute.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.languageDataStore by preferencesDataStore(name = "language_settings")

object LanguagePreferenceManager {
    private val LANGUAGE_KEY = stringPreferencesKey("language")

    fun languageFlow(context: Context): Flow<String> =
        context.languageDataStore.data.map { prefs ->
            prefs[LANGUAGE_KEY] ?: "el"
        }

    suspend fun setLanguage(context: Context, language: String) {
        context.languageDataStore.edit { prefs ->
            prefs[LANGUAGE_KEY] = language
        }
    }
}
