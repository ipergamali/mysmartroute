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
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun applyTheme(context: Context, theme: AppTheme, dark: Boolean) {
        viewModelScope.launch {
            ThemePreferenceManager.setTheme(context, theme)
            ThemePreferenceManager.setDarkTheme(context, dark)

            val userId = auth.currentUser?.uid ?: return@launch
            val dao = MySmartRouteDatabase.getInstance(context).settingsDao()
            val entity = SettingsEntity(userId, theme.name, dark)
            dao.insert(entity)

            if (NetworkUtils.isInternetAvailable(context)) {
                val data = mapOf(
                    "theme" to theme.name,
                    "darkTheme" to dark
                )
                db.collection("user_settings").document(userId).set(data)
            }
        }
    }
}
