package com.ioannapergamali.mysmartroute.utils

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.launchDataStore by preferencesDataStore(name = "launch_settings")

object FirstLaunchManager {
    private val FIRST_LAUNCH_KEY = booleanPreferencesKey("first_launch")

    suspend fun isFirstLaunch(context: Context): Boolean =
        context.launchDataStore.data.map { prefs ->
            prefs[FIRST_LAUNCH_KEY] ?: true
        }.first()

    suspend fun setFirstLaunch(context: Context, value: Boolean) {
        context.launchDataStore.edit { prefs ->
            prefs[FIRST_LAUNCH_KEY] = value
        }
    }
}
