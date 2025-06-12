package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.SettingsEntity
import com.ioannapergamali.mysmartroute.view.ui.AppTheme
import com.ioannapergamali.mysmartroute.utils.NetworkUtils
import kotlinx.coroutines.tasks.await
import com.ioannapergamali.mysmartroute.utils.ThemePreferenceManager
import com.ioannapergamali.mysmartroute.utils.FontPreferenceManager
import com.ioannapergamali.mysmartroute.view.ui.AppFont
import com.ioannapergamali.mysmartroute.utils.SoundPreferenceManager
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private suspend fun updateSettings(
        context: Context,
        transform: (SettingsEntity) -> SettingsEntity
    ) {
        val userId = auth.currentUser?.uid ?: return
        val dao = MySmartRouteDatabase.getInstance(context).settingsDao()
        val current = dao.getSettings(userId) ?: SettingsEntity(
            userId = userId,
            theme = AppTheme.Ocean.name,
            darkTheme = false,
            font = AppFont.SansSerif.name,
            soundEnabled = true,
            soundVolume = 1f
        )
        val updated = transform(current)
        try {
            dao.insert(updated)
        } catch (e: Exception) {
            Log.e("SettingsViewModel", "Αποτυχία τοπικής αποθήκευσης", e)
            return
        }

        if (NetworkUtils.isInternetAvailable(context)) {
            try {
                db.collection("user_settings")
                    .document(userId)
                    .set(updated)
                    .await()
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Αποτυχία αποθήκευσης στο Firestore", e)
            }
        }
    }

    fun applyTheme(context: Context, theme: AppTheme, dark: Boolean) {
        viewModelScope.launch {
            ThemePreferenceManager.setTheme(context, theme)
            ThemePreferenceManager.setDarkTheme(context, dark)
            updateSettings(context) { it.copy(theme = theme.name, darkTheme = dark) }
        }
    }

    fun applyFont(context: Context, font: AppFont) {
        viewModelScope.launch {
            FontPreferenceManager.setFont(context, font)
            updateSettings(context) { it.copy(font = font.name) }
        }
    }

    fun applySoundEnabled(context: Context, enabled: Boolean) {
        viewModelScope.launch {
            SoundPreferenceManager.setSoundEnabled(context, enabled)
            updateSettings(context) { it.copy(soundEnabled = enabled) }
        }
    }

    fun applySoundVolume(context: Context, volume: Float) {
        viewModelScope.launch {
            SoundPreferenceManager.setSoundVolume(context, volume)
            updateSettings(context) { it.copy(soundVolume = volume) }
        }
    }

    fun syncSettings(context: Context) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            val dao = MySmartRouteDatabase.getInstance(context).settingsDao()

            val local = dao.getSettings(userId)
            val remote = if (NetworkUtils.isInternetAvailable(context)) {
                try {
                    val doc = db.collection("user_settings").document(userId).get().await()
                    if (doc.exists()) {
                        SettingsEntity(
                            userId = userId,
                            theme = doc.getString("theme") ?: AppTheme.Ocean.name,
                            darkTheme = doc.getBoolean("darkTheme") ?: false,
                            font = doc.getString("font") ?: AppFont.SansSerif.name,
                            soundEnabled = doc.getBoolean("soundEnabled") ?: true,
                            soundVolume = (doc.getDouble("soundVolume") ?: 1.0).toFloat()
                        )
                    } else null
                } catch (_: Exception) {
                    null
                }
            } else null

            val settings = remote ?: local ?: return@launch

            dao.insert(settings)
            ThemePreferenceManager.setTheme(context, AppTheme.valueOf(settings.theme))
            ThemePreferenceManager.setDarkTheme(context, settings.darkTheme)
            FontPreferenceManager.setFont(context, AppFont.valueOf(settings.font))
            SoundPreferenceManager.setSoundEnabled(context, settings.soundEnabled)
            SoundPreferenceManager.setSoundVolume(context, settings.soundVolume)
        }
    }
}
