package com.ioannapergamali.mysmartroute.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ioannapergamali.mysmartroute.model.enumerations.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

private val Context.languageDataStore by preferencesDataStore(name = "language_settings")

object LanguagePreferenceManager {
    private val LANG_KEY = stringPreferencesKey("language")

    fun languageFlow(context: Context): Flow<AppLanguage> =
        context.languageDataStore.data.map { prefs ->
            val name = prefs[LANG_KEY] ?: AppLanguage.ENGLISH.name
            AppLanguage.values().firstOrNull { it.name == name } ?: AppLanguage.ENGLISH
        }

    suspend fun setLanguage(context: Context, language: AppLanguage) {
        context.languageDataStore.edit { prefs ->
            prefs[LANG_KEY] = language.name
        }
    }

    fun applyLanguage(context: Context, language: AppLanguage) {
        val locale = language.locale
        Locale.setDefault(locale)
        val resources = context.resources
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}
