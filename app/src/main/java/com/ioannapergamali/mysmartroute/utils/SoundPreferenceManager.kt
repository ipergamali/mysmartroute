package com.ioannapergamali.mysmartroute.utils

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.soundDataStore by preferencesDataStore(name = "sound_settings")

object SoundPreferenceManager {
    private val SOUND_KEY = booleanPreferencesKey("sound_enabled")
    private val VOLUME_KEY = floatPreferencesKey("sound_volume")

    fun soundEnabledFlow(context: Context): Flow<Boolean> =
        context.soundDataStore.data.map { prefs ->
            prefs[SOUND_KEY] ?: true
        }

    fun soundVolumeFlow(context: Context): Flow<Float> =
        context.soundDataStore.data.map { prefs ->
            prefs[VOLUME_KEY] ?: 1f
        }

    suspend fun getSoundEnabled(context: Context): Boolean =
        soundEnabledFlow(context).first()

    suspend fun getSoundVolume(context: Context): Float =
        soundVolumeFlow(context).first()

    suspend fun setSoundEnabled(context: Context, enabled: Boolean) {
        context.soundDataStore.edit { prefs ->
            prefs[SOUND_KEY] = enabled
        }
    }

    suspend fun setSoundVolume(context: Context, volume: Float) {
        context.soundDataStore.edit { prefs ->
            prefs[VOLUME_KEY] = volume
        }
    }
}
