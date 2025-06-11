package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.view.ui.AppTheme
import com.ioannapergamali.mysmartroute.viewmodel.SettingsViewModel
import com.ioannapergamali.mysmartroute.utils.ThemePreferenceManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemePickerScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel: SettingsViewModel = viewModel()
    val currentTheme by ThemePreferenceManager.themeFlow(context).collectAsState(initial = AppTheme.Ocean)
    val currentDark by ThemePreferenceManager.darkThemeFlow(context).collectAsState(initial = false)

    var expanded = remember { mutableStateOf(false) }
    var selectedTheme = remember { mutableStateOf(currentTheme) }
    var dark = remember { mutableStateOf(currentDark) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Themes")
        ExposedDropdownMenuBox(expanded = expanded.value, onExpandedChange = { expanded.value = !expanded.value }) {
            TextField(
                readOnly = true,
                value = selectedTheme.value.label,
                onValueChange = {},
                label = { Text("Theme") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded.value) },
                modifier = Modifier.menuAnchor()
            )
            ExposedDropdownMenu(expanded = expanded.value, onDismissRequest = { expanded.value = false }) {
                AppTheme.values().forEach { theme ->
                    DropdownMenuItem(text = { Text(theme.label) }, onClick = {
                        selectedTheme.value = theme
                        expanded.value = false
                        scope.launch { ThemePreferenceManager.setTheme(context, theme) }
                    })
                }
            }
        }
        Text("Dark Theme")
        Switch(checked = dark.value, onCheckedChange = {
            dark.value = it
            scope.launch { ThemePreferenceManager.setDarkTheme(context, it) }
        })

        Button(onClick = {
            viewModel.applyTheme(context, selectedTheme.value, dark.value)
            navController.popBackStack()
        }, modifier = Modifier.padding(top = 16.dp)) {
            Text("Apply")
        }
    }
}
