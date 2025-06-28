package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.utils.toFirestoreMap
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.SettingsEntity
import com.ioannapergamali.mysmartroute.data.local.UserEntity
import com.ioannapergamali.mysmartroute.data.local.insertSettingsSafely
import com.ioannapergamali.mysmartroute.view.ui.AppTheme
import com.ioannapergamali.mysmartroute.utils.NetworkUtils
import kotlinx.coroutines.tasks.await
import com.ioannapergamali.mysmartroute.utils.ThemePreferenceManager
import com.ioannapergamali.mysmartroute.utils.FontPreferenceManager
import com.ioannapergamali.mysmartroute.view.ui.AppFont
import com.ioannapergamali.mysmartroute.model.interfaces.ThemeOption
import com.ioannapergamali.mysmartroute.utils.SoundPreferenceManager
import com.ioannapergamali.mysmartroute.utils.LanguagePreferenceManager
import com.ioannapergamali.mysmartroute.utils.LocaleUtils
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
            val message = "Δεν βρέθηκε χρήστης"
            Log.w("SettingsViewModel", message)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
            return
        }
        val dbLocal = MySmartRouteDatabase.getInstance(context)

        val userDao = dbLocal.userDao()
        val dao = dbLocal.settingsDao()
        val current = dao.getSettings(userId) ?: SettingsEntity(
            userId = userId,
            theme = AppTheme.Ocean.name,
            darkTheme = false,
            font = AppFont.SansSerif.name,
            soundEnabled = true,
            soundVolume = 1f,
            language = "el"
        )
        val updated = transform(current)
        val checkingMessage = "Γίνονται έλεγχοι αποθήκευσης"
        Log.d("SettingsViewModel", checkingMessage)
        withContext(Dispatchers.Main) {
            Toast.makeText(context, checkingMessage, Toast.LENGTH_SHORT).show()
        }
        try {
            insertSettingsSafely(dao, userDao, updated)
            Log.d("SettingsViewModel", "Τοπική αποθήκευση επιτυχής: $updated")
            val localSuccess = "Αποθηκεύτηκε τοπικά"
            Log.d("SettingsViewModel", localSuccess)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, localSuccess, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            val errorMessage = "Σφάλμα τοπικής αποθήκευσης: ${e.localizedMessage}"
            Log.e("SettingsViewModel", errorMessage, e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            }
            return
        }

        if (NetworkUtils.isInternetAvailable(context)) {
            try {
                val data = updated.toFirestoreMap()
                db.collection("user_settings")
                    .document(userId)
                    .set(data)
                    .await()
                Log.d("SettingsViewModel", "Αποθήκευση στο Firestore επιτυχής")
                val cloudSuccess = "Αποθηκεύτηκε στο cloud"
                Log.d("SettingsViewModel", cloudSuccess)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, cloudSuccess, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                val cloudError = "Σφάλμα cloud αποθήκευσης: ${e.localizedMessage}"
                Log.e("SettingsViewModel", cloudError, e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, cloudError, Toast.LENGTH_LONG).show()
                }
            }
        } else {
            val noInternet = "Αποθήκευση μόνο τοπικά"
            Log.d("SettingsViewModel", "Δεν υπάρχει σύνδεση, παράλειψη cloud")
            Log.d("SettingsViewModel", noInternet)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, noInternet, Toast.LENGTH_SHORT).show()
            }
        }
    }

    suspend fun applyAllSettings(
        context: Context,
        theme: ThemeOption,
        dark: Boolean,
        font: AppFont,
        soundEnabled: Boolean,
        soundVolume: Float
    ) {
        ThemePreferenceManager.setTheme(context, theme)
        ThemePreferenceManager.setDarkTheme(context, dark)
        FontPreferenceManager.setFont(context, font)
        SoundPreferenceManager.setSoundEnabled(context, soundEnabled)
        SoundPreferenceManager.setSoundVolume(context, soundVolume)
        val language = LanguagePreferenceManager.languageFlow(context).first()
        updateSettings(context) {
            it.copy(
                theme = (theme as? AppTheme)?.name ?: theme.label,
                darkTheme = dark,
                font = font.name,
                soundEnabled = soundEnabled,
                soundVolume = soundVolume,
                language = language
            )
        }
    }

    fun applyTheme(context: Context, theme: ThemeOption, dark: Boolean) {
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

    fun applyLanguage(context: Context, language: String) {
        viewModelScope.launch {
            LanguagePreferenceManager.setLanguage(context, language)
            LocaleUtils.updateLocale(context, language)
        }
    }

    fun syncSettings(context: Context) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            val dbLocal = MySmartRouteDatabase.getInstance(context)
            val userDao = dbLocal.userDao()
            if (userDao.getUser(userId) == null) {
                userDao.insert(UserEntity(id = userId))
            }
            val dao = dbLocal.settingsDao()

            val local = dao.getSettings(userId)
            val remote = if (NetworkUtils.isInternetAvailable(context)) {
                try {
                    val doc = db.collection("user_settings").document(userId).get().await()
                    if (doc.exists()) {
                        val refId = doc.getDocumentReference("userId")?.id ?: userId
                        SettingsEntity(
                            userId = refId,
                            theme = doc.getString("theme") ?: AppTheme.Ocean.name,
                            darkTheme = doc.getBoolean("darkTheme") ?: false,
                            font = doc.getString("font") ?: AppFont.SansSerif.name,
                            soundEnabled = doc.getBoolean("soundEnabled") ?: true,
                            soundVolume = (doc.getDouble("soundVolume") ?: 1.0).toFloat(),
                            language = doc.getString("language") ?: "el"
                        )
                    } else null
                } catch (_: Exception) {
                    null
                }
            } else null

            val settings = remote ?: local ?: return@launch

            insertSettingsSafely(dao, userDao, settings)
            ThemePreferenceManager.setTheme(context, AppTheme.valueOf(settings.theme))
            ThemePreferenceManager.setDarkTheme(context, settings.darkTheme)
            FontPreferenceManager.setFont(context, AppFont.valueOf(settings.font))
            SoundPreferenceManager.setSoundEnabled(context, settings.soundEnabled)
            SoundPreferenceManager.setSoundVolume(context, settings.soundVolume)
            LanguagePreferenceManager.setLanguage(context, settings.language)
            LocaleUtils.updateLocale(context, settings.language)
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
            val language = LanguagePreferenceManager.languageFlow(context).first()

            // Αν δεν υπάρχει συνδεδεμένος χρήστης, αποθηκεύουμε μόνο τοπικά
            if (auth.currentUser?.uid == null) {
                ThemePreferenceManager.setTheme(context, theme)
                ThemePreferenceManager.setDarkTheme(context, dark)
                FontPreferenceManager.setFont(context, font)
                SoundPreferenceManager.setSoundEnabled(context, soundEnabled)
                SoundPreferenceManager.setSoundVolume(context, soundVolume)
                LanguagePreferenceManager.setLanguage(context, language)
                LocaleUtils.updateLocale(context, language)
                Log.d("SettingsViewModel", "Αποθήκευση μόνο τοπικά καθώς δεν βρέθηκε χρήστης")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Αποθηκεύτηκαν τοπικά", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }
            updateSettings(context) {
                it.copy(
                    theme = (theme as? AppTheme)?.name ?: theme.label,
                    darkTheme = dark,
                    font = font.name,
                    soundEnabled = soundEnabled,
                    soundVolume = soundVolume,
                    language = language
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
                    soundVolume = 1f,
                    language = "el"
                )
            }
        }
    }
}
