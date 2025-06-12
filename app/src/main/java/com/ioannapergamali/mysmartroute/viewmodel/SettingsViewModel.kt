package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import android.util.Log
import android.widget.Toast
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class SettingsViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private suspend fun updateSettings(
        context: Context,
        transform: (SettingsEntity) -> SettingsEntity
    ) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.w("SettingsViewModel", "Δεν βρέθηκε συνδεδεμένος χρήστης")
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Δεν βρέθηκε χρήστης", Toast.LENGTH_SHORT).show()
            }
            return
        }
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
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Γίνονται έλεγχοι αποθήκευσης", Toast.LENGTH_SHORT).show()
        }
        try {
            dao.insert(updated)
            Log.d("SettingsViewModel", "Τοπική αποθήκευση επιτυχής: $updated")
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Αποθηκεύτηκε τοπικά", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("SettingsViewModel", "Αποτυχία τοπικής αποθήκευσης", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Σφάλμα τοπικής αποθήκευσης: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
            return
        }

        if (NetworkUtils.isInternetAvailable(context)) {
            try {
                db.collection("user_settings")
                    .document(userId)
                    .set(updated)
                    .await()
                Log.d("SettingsViewModel", "Αποθήκευση στο Firestore επιτυχής")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Αποθηκεύτηκε στο cloud", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Αποτυχία αποθήκευσης στο Firestore", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Σφάλμα cloud αποθήκευσης: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            Log.d("SettingsViewModel", "Δεν υπάρχει σύνδεση, παράλειψη cloud")
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Αποθήκευση μόνο τοπικά", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun applyAllSettings(
        context: Context,
        theme: AppTheme,
        dark: Boolean,
        font: AppFont,
        soundEnabled: Boolean,
        soundVolume: Float
    ) {
        viewModelScope.launch {
            ThemePreferenceManager.setTheme(context, theme)
            ThemePreferenceManager.setDarkTheme(context, dark)
            FontPreferenceManager.setFont(context, font)
            SoundPreferenceManager.setSoundEnabled(context, soundEnabled)
            SoundPreferenceManager.setSoundVolume(context, soundVolume)
            updateSettings(context) {
                it.copy(
                    theme = theme.name,
                    darkTheme = dark,
                    font = font.name,
                    soundEnabled = soundEnabled,
                    soundVolume = soundVolume
                )
            }
        }
    }

    fun applyTheme(context: Context, theme: AppTheme, dark: Boolean) {
        viewModelScope.launch {
            ThemePreferenceManager.setTheme(context, theme)
            ThemePreferenceManager.setDarkTheme(context, dark)
        }
    }

    fun applyFont(context: Context, font: AppFont) {
        viewModelScope.launch {
            FontPreferenceManager.setFont(context, font)
        }
    }

    fun applySoundEnabled(context: Context, enabled: Boolean) {
        viewModelScope.launch {
            SoundPreferenceManager.setSoundEnabled(context, enabled)
        }
    }

    fun applySoundVolume(context: Context, volume: Float) {
        viewModelScope.launch {
            SoundPreferenceManager.setSoundVolume(context, volume)
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

    fun saveCurrentSettings(context: Context) {
        viewModelScope.launch {
            Log.d("SettingsViewModel", "Εκκίνηση αποθήκευσης ρυθμίσεων")
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Αποθήκευση ρυθμίσεων...", Toast.LENGTH_SHORT).show()
            }
            val theme = ThemePreferenceManager.themeFlow(context).first()
            val dark = ThemePreferenceManager.darkThemeFlow(context).first()
            val font = FontPreferenceManager.fontFlow(context).first()
            val soundEnabled = SoundPreferenceManager.getSoundEnabled(context)
            val soundVolume = SoundPreferenceManager.getSoundVolume(context)
            updateSettings(context) {
                it.copy(
                    theme = theme.name,
                    darkTheme = dark,
                    font = font.name,
                    soundEnabled = soundEnabled,
                    soundVolume = soundVolume
                )
            }
        }
    }

    fun resetSettings(context: Context) {
        viewModelScope.launch {
            Log.d("SettingsViewModel", "Επαναφορά ρυθμίσεων στα προεπιλεγμένα")
            updateSettings(context) {
                it.copy(
                    theme = AppTheme.Ocean.name,
                    darkTheme = false,
                    font = AppFont.SansSerif.name,
                    soundEnabled = true,
                    soundVolume = 1f
                )
            }
        }
    }
}
