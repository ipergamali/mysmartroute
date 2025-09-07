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

/**
 * Διαχείριση προτιμήσεων ήχου (ενεργοποίηση και ένταση).
 * Manages sound preferences (enabled state and volume).
 */
object SoundPreferenceManager {
    private val SOUND_KEY = booleanPreferencesKey("sound_enabled")
    private val VOLUME_KEY = floatPreferencesKey("sound_volume")

    /**
     * Παρακολουθεί αν ο ήχος είναι ενεργοποιημένος.
     * Observes whether sound is enabled.
     */
    fun soundEnabledFlow(context: Context): Flow<Boolean> =
        context.soundDataStore.data.map { prefs ->
            prefs[SOUND_KEY] ?: true
        }

    /**
     * Παρακολουθεί την επιλεγμένη ένταση.
     * Observes the selected volume.
     */
    fun soundVolumeFlow(context: Context): Flow<Float> =
        context.soundDataStore.data.map { prefs ->
            prefs[VOLUME_KEY] ?: 1f
        }

    /**
     * Επιστρέφει αν ο ήχος είναι ενεργοποιημένος.
     * Returns whether sound is enabled.
     */
    suspend fun getSoundEnabled(context: Context): Boolean =
        soundEnabledFlow(context).first()

    /**
     * Επιστρέφει την τρέχουσα ένταση.
     * Returns the current volume.
     */
    suspend fun getSoundVolume(context: Context): Float =
        soundVolumeFlow(context).first()

    /**
     * Αποθηκεύει αν ο ήχος είναι ενεργός.
     * Saves whether sound is enabled.
     */
    suspend fun setSoundEnabled(context: Context, enabled: Boolean) {
        context.soundDataStore.edit { prefs ->
            prefs[SOUND_KEY] = enabled
        }
    }

    /**
     * Αποθηκεύει τη νέα ένταση.
     * Saves the new volume.
     */
    suspend fun setSoundVolume(context: Context, volume: Float) {
        context.soundDataStore.edit { prefs ->
            prefs[VOLUME_KEY] = volume
        }
    }
}
