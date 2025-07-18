package com.ioannapergamali.mysmartroute.viewmodel

import android.os.Bundle
import android.util.Log
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.lifecycleScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.model.navigation.NavigationHost
import com.ioannapergamali.mysmartroute.view.ui.MysmartrouteTheme
import com.ioannapergamali.mysmartroute.view.ui.AppTheme
import com.ioannapergamali.mysmartroute.view.ui.components.DrawerWrapper
import com.ioannapergamali.mysmartroute.view.ui.AppFont
import com.ioannapergamali.mysmartroute.utils.FontPreferenceManager
import com.ioannapergamali.mysmartroute.utils.MiuiUtils
import com.ioannapergamali.mysmartroute.utils.ThemePreferenceManager
import com.ioannapergamali.mysmartroute.utils.SoundPreferenceManager
import com.ioannapergamali.mysmartroute.utils.SoundManager
import com.ioannapergamali.mysmartroute.viewmodel.SettingsViewModel
import com.ioannapergamali.mysmartroute.utils.MapsUtils
import com.ioannapergamali.mysmartroute.utils.LanguagePreferenceManager
import com.ioannapergamali.mysmartroute.utils.LocaleUtils
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import com.ioannapergamali.mysmartroute.model.interfaces.ThemeOption
import android.view.WindowManager



class MainActivity : ComponentActivity() {
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    override fun onCreate(savedInstanceState : Bundle?)
    {
        // Εφαρμογή αποθηκευμένης γλώσσας πριν αρχίσει το lifecycle
        val startLang = runBlocking { LanguagePreferenceManager.getLanguage(this@MainActivity) }
        LocaleUtils.updateLocale(this, startLang)
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, locationPermissions, 0)
        }
        // Προαιρετικός έλεγχος ύπαρξης του MIUI Service Delivery provider
        MiuiUtils.callServiceDelivery(this, "ping")
        // Συγχρονισμός ρυθμίσεων από τη βάση
        settingsViewModel.syncSettings(this)
        // Έλεγχος φόρτωσης του Maps API key
        val apiKey = MapsUtils.getApiKey(this)
        Log.d("Maps", "API key loaded? ${apiKey.isNotEmpty()}")
        // Initialize the soundtrack and start playback based on saved preferences.
        SoundManager.initialize(applicationContext)
        lifecycleScope.launch {
            val enabled = SoundPreferenceManager.getSoundEnabled(applicationContext)
            val volume = SoundPreferenceManager.getSoundVolume(applicationContext)
            SoundManager.setVolume(volume)
            if (enabled) SoundManager.play()
        }
        setContent {
            val context = LocalContext.current
            val theme by ThemePreferenceManager.themeFlow(context).collectAsState(initial = AppTheme.Ocean)
            val dark by ThemePreferenceManager.darkThemeFlow(context).collectAsState(initial = false)
            val font by FontPreferenceManager.fontFlow(context).collectAsState(initial = AppFont.SansSerif)
            val soundEnabled by SoundPreferenceManager.soundEnabledFlow(context).collectAsState(initial = true)
            val soundVolume by SoundPreferenceManager.soundVolumeFlow(context).collectAsState(initial = 1f)
            val language by LanguagePreferenceManager.languageFlow(context).collectAsState(initial = "el")

            LaunchedEffect(language) {
                LocaleUtils.updateLocale(context, language)
            }

            LaunchedEffect(soundEnabled, soundVolume) {
                SoundManager.setVolume(soundVolume)
                if (soundEnabled) {
                    if (!SoundManager.isPlaying) SoundManager.play()
                } else {
                    if (SoundManager.isPlaying) SoundManager.pause()
                }
            }

            MysmartrouteTheme(theme = theme, darkTheme = dark, font = font.fontFamily) {
                val navController = rememberNavController()
                DrawerWrapper(navController = navController) { openDrawer ->
                    NavigationHost(navController = navController, openDrawer = openDrawer)
                }
            }
        }
    }

    override fun onBackPressed() {
        finishAfterTransition()
        super.onBackPressed()
    }

    override fun onDestroy() {
        SoundManager.release()
        super.onDestroy()
    }
}
