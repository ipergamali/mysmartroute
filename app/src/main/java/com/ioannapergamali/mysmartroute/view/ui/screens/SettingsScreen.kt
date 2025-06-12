package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.TextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.widget.Toast
import androidx.navigation.NavController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import com.ioannapergamali.mysmartroute.utils.SoundManager
import com.ioannapergamali.mysmartroute.utils.SoundPreferenceManager
import com.ioannapergamali.mysmartroute.utils.ThemePreferenceManager
import com.ioannapergamali.mysmartroute.utils.FontPreferenceManager
import com.ioannapergamali.mysmartroute.view.ui.AppFont
import com.ioannapergamali.mysmartroute.view.ui.AppTheme
import com.ioannapergamali.mysmartroute.view.ui.MysmartrouteTheme
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.viewmodel.SettingsViewModel

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsScreen(navController: NavController, openDrawer: () -> Unit) {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel()

    val currentTheme by ThemePreferenceManager.themeFlow(context).collectAsState(initial = AppTheme.Ocean)
    val currentDark by ThemePreferenceManager.darkThemeFlow(context).collectAsState(initial = false)
    val currentFont by FontPreferenceManager.fontFlow(context).collectAsState(initial = AppFont.SansSerif)
    val soundEnabled by SoundPreferenceManager.soundEnabledFlow(context).collectAsState(initial = true)
    val currentVolume by SoundPreferenceManager.soundVolumeFlow(context).collectAsState(initial = 1f)

    val expandedTheme = remember { mutableStateOf(false) }
    val selectedTheme = remember { mutableStateOf(currentTheme) }
    val dark = remember { mutableStateOf(currentDark) }

    val expandedFont = remember { mutableStateOf(false) }
    val selectedFont = remember { mutableStateOf(currentFont) }

    val soundState = remember { mutableStateOf(soundEnabled) }
    val volumeState = remember { mutableFloatStateOf(currentVolume) }

    val saveChecked = remember { mutableStateOf(false) }

    LaunchedEffect(currentTheme) { selectedTheme.value = currentTheme }
    LaunchedEffect(currentDark) { dark.value = currentDark }
    LaunchedEffect(currentFont) { selectedFont.value = currentFont }
    LaunchedEffect(soundEnabled) { soundState.value = soundEnabled }
    LaunchedEffect(currentVolume) { volumeState.floatValue = currentVolume }

    MysmartrouteTheme(
        theme = selectedTheme.value,
        darkTheme = dark.value,
        font = selectedFont.value.fontFamily
    ) {
        Scaffold(
            topBar = {
                TopBar(
                    title = "Settings",
                    navController = navController,
                    showMenu = true,
                    onMenuClick = openDrawer
                )
            }
        ) { padding ->
            ScreenContainer(modifier = Modifier.padding(padding)) {
            Text("Θέμα")
            Divider(modifier = Modifier.padding(vertical = 4.dp))
            ExposedDropdownMenuBox(expanded = expandedTheme.value, onExpandedChange = { expandedTheme.value = !expandedTheme.value }) {
                TextField(
                    readOnly = true,
                    value = selectedTheme.value.label,
                    onValueChange = {},
                    label = { Text("Theme") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTheme.value) },
                    modifier = Modifier.menuAnchor()
                )
                DropdownMenu(expanded = expandedTheme.value, onDismissRequest = { expandedTheme.value = false }) {
                    AppTheme.values().forEach { theme ->
                        DropdownMenuItem(text = { Text(theme.label) }, onClick = {
                            selectedTheme.value = theme
                            expandedTheme.value = false
                        })
                    }
                }
            }
            Text("Dark Theme")
            Switch(checked = dark.value, onCheckedChange = { dark.value = it })

            Text("Γραμματοσειρά", modifier = Modifier.padding(top = 16.dp))
            Divider(modifier = Modifier.padding(vertical = 4.dp))
            ExposedDropdownMenuBox(expanded = expandedFont.value, onExpandedChange = { expandedFont.value = !expandedFont.value }) {
                TextField(
                    readOnly = true,
                    value = selectedFont.value.label,
                    onValueChange = {},
                    label = { Text("Fonts") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFont.value) },
                    modifier = Modifier.menuAnchor()
                )
                DropdownMenu(expanded = expandedFont.value, onDismissRequest = { expandedFont.value = false }) {
                    AppFont.values().forEach { font ->
                        DropdownMenuItem(text = { Text(font.label) }, onClick = {
                            selectedFont.value = font
                            expandedFont.value = false
                        })
                    }
                }
            }

            Text("Ήχος", modifier = Modifier.padding(top = 16.dp))
            Divider(modifier = Modifier.padding(vertical = 4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    val newState = !soundState.value
                    soundState.value = newState
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
                    },
                    valueRange = 0f..1f,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 16.dp)) {
                Checkbox(
                    checked = saveChecked.value,
                    onCheckedChange = { saveChecked.value = it }
                )
                Text("Οριστική αποθήκευση επιλογών", modifier = Modifier.padding(start = 8.dp))
            }

            Button(
                onClick = {
                    viewModel.applyTheme(context, selectedTheme.value, dark.value)
                    viewModel.applyFont(context, selectedFont.value)
                    viewModel.applySoundEnabled(context, soundState.value)
                    viewModel.applySoundVolume(context, volumeState.floatValue)

                    if (saveChecked.value) {
                        viewModel.saveCurrentSettings(context)
                    } else {
                        Toast.makeText(context, "Επιλέξτε το κουτάκι πριν πατήσετε Apply", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Apply")
            }
        }
    }
}
}
