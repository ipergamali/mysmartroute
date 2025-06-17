package com.ioannapergamali.mysmartroute.viewmodel

import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.lifecycleScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
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
import com.ioannapergamali.mysmartroute.utils.FirstLaunchManager
import com.ioannapergamali.mysmartroute.viewmodel.SettingsViewModel
import com.ioannapergamali.mysmartroute.viewmodel.AuthenticationViewModel
import kotlinx.coroutines.launch



class MainActivity : ComponentActivity() {
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val authViewModel: AuthenticationViewModel by viewModels()

    private val ADMIN_PASSWORD = "SECRET123" // Βάλτε εδώ τον μυστικό κωδικό
    override fun onCreate(savedInstanceState : Bundle?)
    {
        super.onCreate(savedInstanceState)
        // Προαιρετικός έλεγχος ύπαρξης του MIUI Service Delivery provider
        MiuiUtils.callServiceDelivery(this, "ping")
        // Συγχρονισμός ρυθμίσεων από τη βάση
        settingsViewModel.syncSettings(this)
        // Initialize the soundtrack and start playback based on saved preferences.
        SoundManager.initialize(applicationContext)
        lifecycleScope.launch {
            val enabled = SoundPreferenceManager.getSoundEnabled(applicationContext)
            val volume = SoundPreferenceManager.getSoundVolume(applicationContext)
            SoundManager.setVolume(volume)
            if (enabled) SoundManager.play()
            if (FirstLaunchManager.isFirstLaunch(this@MainActivity)) {
                showAdminSetupDialog()
                FirstLaunchManager.setFirstLaunch(this@MainActivity, false)
            }
        }
        setContent {
            val context = LocalContext.current
            val theme by ThemePreferenceManager.themeFlow(context).collectAsState(initial = AppTheme.Ocean)
            val dark by ThemePreferenceManager.darkThemeFlow(context).collectAsState(initial = false)
            val font by FontPreferenceManager.fontFlow(context).collectAsState(initial = AppFont.SansSerif)
            val soundEnabled by SoundPreferenceManager.soundEnabledFlow(context).collectAsState(initial = true)
            val soundVolume by SoundPreferenceManager.soundVolumeFlow(context).collectAsState(initial = 1f)

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

    private fun showAdminSetupDialog() {
        val input = EditText(this)
        AlertDialog.Builder(this)
            .setTitle("Admin Setup")
            .setMessage("Εισάγετε κωδικό διαχειριστή")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val text = input.text.toString()
                if (text == ADMIN_PASSWORD) {
                    authViewModel.seedAdminAccount(this, text)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
