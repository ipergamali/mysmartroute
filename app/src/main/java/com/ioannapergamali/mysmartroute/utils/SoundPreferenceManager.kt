package com.ioannapergamali.mysmartroute.utils

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.soundDataStore by preferencesDataStore(name = "sound_settings")

object SoundPreferenceManager {
    private val SOUND_KEY = booleanPreferencesKey("sound_enabled")

    fun soundEnabledFlow(context: Context): Flow<Boolean> =
        context.soundDataStore.data.map { prefs ->
            prefs[SOUND_KEY] ?: true
        }

    suspend fun setSoundEnabled(context: Context, enabled: Boolean) {
        context.soundDataStore.edit { prefs ->
            prefs[SOUND_KEY] = enabled
        }
    }
}
