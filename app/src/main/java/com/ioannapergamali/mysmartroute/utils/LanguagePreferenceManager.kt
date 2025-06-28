package com.ioannapergamali.mysmartroute.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ioannapergamali.mysmartroute.data.local.LanguageSettingEntity
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.languageDataStore by preferencesDataStore(name = "language_settings")

object LanguagePreferenceManager {
    private val LANGUAGE_KEY = stringPreferencesKey("language")

    fun languageFlow(context: Context): Flow<String> =
        context.languageDataStore.data.map { prefs ->
            prefs[LANGUAGE_KEY] ?: "el"
        }

    suspend fun setLanguage(context: Context, language: String) {
        context.languageDataStore.edit { prefs ->
            prefs[LANGUAGE_KEY] = language
        }
        val db = MySmartRouteDatabase.getInstance(context)
        db.languageSettingDao().insert(LanguageSettingEntity(language = language))
        try {
            FirebaseFirestore.getInstance()
                .collection("app_language")
                .document("current")
                .set(mapOf("language" to language))
                .await()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Η γλώσσα αποθηκεύτηκε στο cloud", Toast.LENGTH_SHORT).show()
            }
        } catch (_: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Σφάλμα αποθήκευσης γλώσσας στο cloud", Toast.LENGTH_SHORT).show()
            }
        }
    }

    suspend fun getLanguage(context: Context): String {
        val db = MySmartRouteDatabase.getInstance(context)
        val stored = db.languageSettingDao().get()?.language
        if (stored != null) {
            context.languageDataStore.edit { prefs ->
                prefs[LANGUAGE_KEY] = stored
            }
            return stored
        }
        val prefs = context.languageDataStore.data.first()
        return prefs[LANGUAGE_KEY] ?: "el"
    }
}
