package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.SettingsEntity
import com.ioannapergamali.mysmartroute.view.ui.AppTheme
import com.ioannapergamali.mysmartroute.utils.NetworkUtils
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
        dao.insert(updated)

        if (NetworkUtils.isInternetAvailable(context)) {
            val data = mapOf(
                "theme" to updated.theme,
                "darkTheme" to updated.darkTheme,
                "font" to updated.font,
                "soundEnabled" to updated.soundEnabled,
                "soundVolume" to updated.soundVolume
            )
            db.collection("user_settings").document(userId).set(data)
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
}
