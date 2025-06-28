package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.ui.Alignment
import com.ioannapergamali.mysmartroute.utils.SoundPreferenceManager
import com.ioannapergamali.mysmartroute.utils.ThemePreferenceManager
import com.ioannapergamali.mysmartroute.utils.FontPreferenceManager
import com.ioannapergamali.mysmartroute.view.ui.MysmartrouteTheme
import com.ioannapergamali.mysmartroute.view.ui.AppTheme
import com.ioannapergamali.mysmartroute.view.ui.AppFont
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import androidx.compose.ui.res.stringResource
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.viewmodel.SettingsViewModel
import androidx.compose.material3.Slider
import com.ioannapergamali.mysmartroute.utils.SoundManager
import com.ioannapergamali.mysmartroute.model.interfaces.ThemeOption

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
            topBar = { TopBar(title = stringResource(R.string.sound), navController = navController) },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            ScreenContainer(modifier = Modifier.padding(padding)) {
                Text("Ένταση")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        val newState = !soundState.value
                        soundState.value = newState
                        viewModel.applySoundEnabled(context, newState)
                        if (newState) SoundManager.play() else SoundManager.pause()
                    }) {
                        Icon(
                            imageVector = if (soundState.value) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                            contentDescription = if (soundState.value) "Ήχος" else "Σίγαση"
                        )
                    }

                    Slider(
                        value = volumeState.floatValue,
                        onValueChange = {
                            volumeState.floatValue = it
                            SoundManager.setVolume(it)
                            viewModel.applySoundVolume(context, it)
                        },
                        valueRange = 0f..1f,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
