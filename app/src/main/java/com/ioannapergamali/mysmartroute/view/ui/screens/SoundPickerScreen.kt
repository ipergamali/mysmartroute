package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.utils.SoundPreferenceManager
import com.ioannapergamali.mysmartroute.utils.ThemePreferenceManager
import com.ioannapergamali.mysmartroute.utils.FontPreferenceManager
import com.ioannapergamali.mysmartroute.view.ui.MysmartrouteTheme
import com.ioannapergamali.mysmartroute.view.ui.AppTheme
import com.ioannapergamali.mysmartroute.view.ui.AppFont
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.SettingsViewModel
import androidx.compose.material3.Slider

@Composable
fun SoundPickerScreen(navController: NavController) {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel()

    val currentTheme by ThemePreferenceManager.themeFlow(context).collectAsState(initial = AppTheme.Ocean)
    val currentDark by ThemePreferenceManager.darkThemeFlow(context).collectAsState(initial = false)
    val currentFont by FontPreferenceManager.fontFlow(context).collectAsState(initial = AppFont.SansSerif)
    val soundEnabled by SoundPreferenceManager.soundEnabledFlow(context).collectAsState(initial = true)
    val currentVolume by SoundPreferenceManager.soundVolumeFlow(context).collectAsState(initial = 1f)

    val soundState = remember { mutableStateOf(soundEnabled) }
    val volumeState = remember { mutableFloatStateOf(currentVolume) }

    LaunchedEffect(soundEnabled) { soundState.value = soundEnabled }
    LaunchedEffect(currentVolume) { volumeState.floatValue = currentVolume }

    MysmartrouteTheme(theme = currentTheme, darkTheme = currentDark, font = currentFont.fontFamily) {
        Scaffold(
            topBar = { TopBar(title = "Sound", navController = navController) },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                Text("Ήχος")
                Switch(
                    checked = soundState.value,
                    onCheckedChange = { soundState.value = it }
                )

                Text("Ένταση", modifier = Modifier.padding(top = 16.dp))
                Slider(
                    value = volumeState.floatValue,
                    onValueChange = { volumeState.floatValue = it },
                    valueRange = 0f..1f
                )

                Button(
                    onClick = {
                        viewModel.applySoundEnabled(context, soundState.value)
                        viewModel.applySoundVolume(context, volumeState.floatValue)
                        navController.popBackStack()
                    },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Apply")
                }
            }
        }
    }
}
