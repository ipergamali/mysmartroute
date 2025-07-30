package com.ioannapergamali.mysmartroute.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.favoritesDataStore by preferencesDataStore(name = "favorites_settings")

object FavoritesPreferenceManager {
    private val PREFERRED_KEY = stringSetPreferencesKey("preferred")
    private val NON_PREFERRED_KEY = stringSetPreferencesKey("non_preferred")

    fun preferredFlow(context: Context): Flow<Set<VehicleType>> =
        context.favoritesDataStore.data.map { prefs ->
            prefs[PREFERRED_KEY]
                ?.mapNotNull { runCatching { VehicleType.valueOf(it) }.getOrNull() }
                ?.toSet() ?: emptySet()
        }

    fun nonPreferredFlow(context: Context): Flow<Set<VehicleType>> =
        context.favoritesDataStore.data.map { prefs ->
            prefs[NON_PREFERRED_KEY]
                ?.mapNotNull { runCatching { VehicleType.valueOf(it) }.getOrNull() }
                ?.toSet() ?: emptySet()
        }

    suspend fun addPreferred(context: Context, type: VehicleType) {
        context.favoritesDataStore.edit { prefs ->
            val preferred = prefs[PREFERRED_KEY]?.toMutableSet() ?: mutableSetOf()
            preferred.add(type.name)
            prefs[PREFERRED_KEY] = preferred
            val non = prefs[NON_PREFERRED_KEY]?.toMutableSet()
            non?.remove(type.name)
            non?.let { prefs[NON_PREFERRED_KEY] = it }
        }
    }

    suspend fun addNonPreferred(context: Context, type: VehicleType) {
        context.favoritesDataStore.edit { prefs ->
            val non = prefs[NON_PREFERRED_KEY]?.toMutableSet() ?: mutableSetOf()
            non.add(type.name)
            prefs[NON_PREFERRED_KEY] = non
            val preferred = prefs[PREFERRED_KEY]?.toMutableSet()
            preferred?.remove(type.name)
            preferred?.let { prefs[PREFERRED_KEY] = it }
        }
    }

    suspend fun removePreferred(context: Context, type: VehicleType) {
        context.favoritesDataStore.edit { prefs ->
            val preferred = prefs[PREFERRED_KEY]?.toMutableSet() ?: mutableSetOf()
            preferred.remove(type.name)
            prefs[PREFERRED_KEY] = preferred
        }
    }

    suspend fun removeNonPreferred(context: Context, type: VehicleType) {
        context.favoritesDataStore.edit { prefs ->
            val non = prefs[NON_PREFERRED_KEY]?.toMutableSet() ?: mutableSetOf()
            non.remove(type.name)
            prefs[NON_PREFERRED_KEY] = non
        }
    }
}
